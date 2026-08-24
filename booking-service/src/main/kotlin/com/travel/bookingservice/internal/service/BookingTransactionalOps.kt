package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.outbox.BookingEventOutboxWriter
import com.travel.bookingservice.internal.persistence.BookingEntity
import com.travel.bookingservice.internal.persistence.BookingRepository
import com.travel.bookingservice.internal.persistence.BookingStatus
import com.travel.common.model.BookingRequest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import kotlin.time.Instant
import kotlin.uuid.Uuid
@Component
class BookingTransactionalOps(val bookingRepository: BookingRepository,
                              val bookingOutboxWriter: BookingEventOutboxWriter,
                              val objectMapper: ObjectMapper,) {

    @Transactional
    fun findOrInsertPending(idempotencyKey: String, request: BookingRequest): Pair<BookingEntity, Boolean>{
        //1. find the bookings against booking id in the repository, if it exists return the booking
        //2. save the booking in BookingEntity
        //3. map the saved entity to saved to true and return
        //4. write the booking to bookingOutbox table
        //5. handle the exception and return false

        bookingRepository.findByIdempotencyKey(idempotencyKey)?.let { return it to true }
       return try{
           val bookingEntity = bookingRepository.saveAndFlush(
            BookingEntity(idempotencyKey = idempotencyKey,
                offerId = request.offerId,
                requestPayload =objectMapper.writeValueAsString(request) )
        )
        bookingOutboxWriter.enqueue("BookingCreated", bookingEntity.bookingId,
            objectMapper.writeValueAsString(BookingCreatedPayload(
                bookingId = bookingEntity.bookingId,
                idempotencyKey = idempotencyKey,
                offerId = request.offerId,
                request=request
            ))
        )
         bookingEntity to false
    }catch (e: DataIntegrityViolationException){
        val entity = bookingRepository.findByIdempotencyKey(idempotencyKey)?:throw  e
           entity to true
    }
    }

    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 4,
        backoff = Backoff(delay = 50, multiplier = 2.0),
    )
    @Transactional
    fun advance(bookingId: Uuid, event: BookingStatusEvent,failureReason:String?=null): BookingEntity{

        //this code will invoke if spring raises ObjectOptimisticLockingFailureException ( T1, T2 updates same version)
        //T2 will get the lock after T1 completes
        //check the state machine
        //if the valid transition is provided then advance
        val booking = bookingRepository.getReferenceById(bookingId)
        booking.status = booking.status.transition(event)
        if(event == BookingStatusEvent.PROVIDER_FAILED) booking.failureReason = failureReason
        booking.updatedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis())

        if(event == BookingStatusEvent.CANCEL_REQUESTED){
            bookingOutboxWriter.enqueue("BookingCancelled",
                bookingId,
                objectMapper.writeValueAsString(
                    BookingCancellationRequestedPayload(bookingId)
                ),)
        }
        return booking

    }

    fun cancel(bookingId:Uuid): BookingEntity{
        try{
            return advance(bookingId,BookingStatusEvent.CANCEL_REQUESTED)
        }catch(ex: IllegalStateTransitionException){

            val bookingEntity =bookingRepository.findByBookingId(bookingId)?: throw BookingNotFoundException(bookingId)
            if(bookingEntity.status == BookingStatus.CANCELLED || bookingEntity.status == BookingStatus.CANCELLATION_PENDING){
                return bookingEntity
            }else{
                throw ex
            }
        }
    }
}

data class BookingCreatedPayload(
    val bookingId: Uuid,
    val idempotencyKey: String,
    val offerId: String,
    val request: BookingRequest,
)

data class BookingCancellationRequestedPayload(val bookingId: Uuid,)