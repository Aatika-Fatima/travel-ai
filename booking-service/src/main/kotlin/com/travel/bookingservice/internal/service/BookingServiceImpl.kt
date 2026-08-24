package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.persistence.BookingRepository
import com.travel.common.model.BookingRequest
import org.springframework.stereotype.Service
import kotlin.uuid.Uuid

@Service
class BookingServiceImpl(val bookingTransactionalOps: BookingTransactionalOps,
                         val bookingRepository: BookingRepository,
    val metrics: BookingMetrics):BookingService {
    override fun submit(
        idempotencyKey: String,
        request: BookingRequest
    ): BookingView {
       val(booking,alreadyExists) = bookingTransactionalOps.findOrInsertPending(idempotencyKey, request)
        if (alreadyExists) metrics.duplicatePrevented("unique_constraint_or_lock")
        return booking.toView(!alreadyExists)
    }

    override fun get(bookingId: Uuid): BookingView {
        val bookingEntity =bookingRepository.findByBookingId(bookingId)?: throw BookingNotFoundException(bookingId)
        return bookingEntity.toView(false)
      }

    override fun cancel(bookingId: Uuid): BookingView {
       return bookingTransactionalOps.cancel(bookingId).toView(true)
    }
}