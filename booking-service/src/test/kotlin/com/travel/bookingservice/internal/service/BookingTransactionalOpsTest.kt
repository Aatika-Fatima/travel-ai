package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.outbox.BookingEventOutboxWriter
import com.travel.bookingservice.internal.persistence.BookingEntity
import com.travel.bookingservice.internal.persistence.BookingRepository
import com.travel.bookingservice.internal.persistence.BookingStatus
import com.travel.common.model.BookingRequest
import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class BookingTransactionalOpsTest {
    private val bookingRepository: BookingRepository = mock()
    private val outboxWriter: BookingEventOutboxWriter = mock()
    private val objectMapper: ObjectMapper = mock()
    private val ops = BookingTransactionalOps(bookingRepository, outboxWriter, objectMapper)

    private fun request(offerId: String = "off_1") =
        BookingRequest(
            offerId = offerId,
            passengers =
                listOf(
                    PassengerDetails(
                        title = "Mr",
                        givenName = "Alex",
                        familyName = "Doe",
                        dateOfBirth = LocalDate.of(1990, 1, 1),
                        gender = "m",
                        email = "alex@example.com",
                        phoneNumber = "+10000000000",
                    ),
                ),
            contact = ContactInfo(email = "alex@example.com", phoneNumber = "+10000000000"),
        )

    private fun booking(
        status: BookingStatus = BookingStatus.INITIATED,
        idempotencyKey: String = "bk-1",
        offerId: String = "off_1",
    ) = BookingEntity(
        idempotencyKey = idempotencyKey,
        offerId = offerId,
        status = status,
        requestPayload = "{}",
    )

    init {
        whenever(objectMapper.writeValueAsString(any())).thenReturn("{}")
    }

    // ---- findOrInsertPending ----

    @Test
    fun `findOrInsertPending returns the existing row when the idempotency key already matches`() {
        val existing = booking()
        whenever(bookingRepository.findByIdempotencyKey("bk-1")).thenReturn(existing)

        val (result, alreadyExisted) = ops.findOrInsertPending("bk-1", request())

        assertEquals(existing, result)
        assertTrue(alreadyExisted)
        verify(bookingRepository, never()).saveAndFlush(any<BookingEntity>())
        verify(outboxWriter, never()).enqueue(any(), any(), any())
    }

    @Test
    fun `findOrInsertPending inserts a new row and enqueues BookingCreated when no key match exists`() {
        whenever(bookingRepository.findByIdempotencyKey("bk-1")).thenReturn(null)
        val saved = booking()
        whenever(bookingRepository.saveAndFlush(any<BookingEntity>())).thenReturn(saved)

        val (result, alreadyExisted) = ops.findOrInsertPending("bk-1", request())

        assertEquals(saved, result)
        assertFalse(alreadyExisted)
        verify(outboxWriter).enqueue(eq("BookingCreated"), eq(saved.bookingId), any())
    }

    @Test
    fun `findOrInsertPending reads back the winner on a lost insert race, without a second insert or event`() {
        val winner = booking()
        whenever(bookingRepository.findByIdempotencyKey("bk-1")).thenReturn(null, winner)
        whenever(bookingRepository.saveAndFlush(any<BookingEntity>()))
            .thenThrow(DataIntegrityViolationException("dup"))

        val (result, alreadyExisted) = ops.findOrInsertPending("bk-1", request())

        assertEquals(winner, result)
        assertTrue(alreadyExisted)
        verify(outboxWriter, never()).enqueue(any(), any(), any())
    }

    @Test
    fun `findOrInsertPending rethrows the original exception when the winner can't be found on re-read`() {
        val ex = DataIntegrityViolationException("dup")
        whenever(bookingRepository.findByIdempotencyKey("bk-1")).thenReturn(null)
        whenever(bookingRepository.saveAndFlush(any<BookingEntity>())).thenThrow(ex)

        val thrown = assertFailsWith<DataIntegrityViolationException> { ops.findOrInsertPending("bk-1", request()) }
        assertEquals(ex, thrown)
    }

    // ---- advance ----

    @Test
    fun `advance applies a valid transition and bumps updatedAt`() {
        val entity = booking(status = BookingStatus.INITIATED)
        val before = entity.updatedAt
        whenever(bookingRepository.getReferenceById(entity.bookingId)).thenReturn(entity)

        val result = ops.advance(entity.bookingId, BookingStatusEvent.PUBLISHED)

        assertEquals(BookingStatus.IN_PROGRESS, result.status)
        assertTrue(result.updatedAt >= before)
        assertNull(result.failureReason)
    }

    @Test
    fun `advance records the failure reason only on PROVIDER_FAILED`() {
        val entity = booking(status = BookingStatus.IN_PROGRESS)
        whenever(bookingRepository.getReferenceById(entity.bookingId)).thenReturn(entity)

        val result = ops.advance(entity.bookingId, BookingStatusEvent.PROVIDER_FAILED, "card_declined")

        assertEquals(BookingStatus.FAILED, result.status)
        assertEquals("card_declined", result.failureReason)
    }

    @Test
    fun `advance does not touch failureReason on non-failure edges`() {
        val entity = booking(status = BookingStatus.IN_PROGRESS)
        whenever(bookingRepository.getReferenceById(entity.bookingId)).thenReturn(entity)

        val result = ops.advance(entity.bookingId, BookingStatusEvent.PROVIDER_RESERVED, "ignored")

        assertEquals(BookingStatus.RESERVED, result.status)
        assertNull(result.failureReason)
    }

    @Test
    fun `advance enqueues BookingCancelled only for CANCEL_REQUESTED`() {
        val entity = booking(status = BookingStatus.INITIATED)
        whenever(bookingRepository.getReferenceById(entity.bookingId)).thenReturn(entity)

        ops.advance(entity.bookingId, BookingStatusEvent.CANCEL_REQUESTED)

        verify(outboxWriter).enqueue(eq("BookingCancelled"), eq(entity.bookingId), any())
    }

    @Test
    fun `advance does not enqueue anything for edges other than CANCEL_REQUESTED`() {
        val entity = booking(status = BookingStatus.INITIATED)
        whenever(bookingRepository.getReferenceById(entity.bookingId)).thenReturn(entity)

        ops.advance(entity.bookingId, BookingStatusEvent.PUBLISHED)

        verify(outboxWriter, never()).enqueue(any(), any(), any())
    }

    @Test
    fun `advance throws and leaves the entity unmodified for a disallowed edge`() {
        val entity = booking(status = BookingStatus.CANCELLED)
        whenever(bookingRepository.getReferenceById(entity.bookingId)).thenReturn(entity)

        assertFailsWith<IllegalStateTransitionException> {
            ops.advance(entity.bookingId, BookingStatusEvent.PUBLISHED)
        }
        assertEquals(BookingStatus.CANCELLED, entity.status)
        verify(outboxWriter, never()).enqueue(any(), any(), any())
    }

    // ---- cancel ----

    @Test
    fun `cancel applies CANCEL_REQUESTED directly when the current state allows it`() {
        val entity = booking(status = BookingStatus.INITIATED)
        whenever(bookingRepository.getReferenceById(entity.bookingId)).thenReturn(entity)

        val result = ops.cancel(entity.bookingId)

        assertEquals(BookingStatus.CANCELLED, result.status)
        verify(outboxWriter).enqueue(eq("BookingCancelled"), eq(entity.bookingId), any())
    }

    @Test
    fun `cancel is idempotent when the booking is already CANCELLED`() {
        val entity = booking(status = BookingStatus.CANCELLED)
        whenever(bookingRepository.getReferenceById(entity.bookingId)).thenReturn(entity)
        whenever(bookingRepository.findByBookingId(entity.bookingId)).thenReturn(entity)

        val result = ops.cancel(entity.bookingId)

        assertEquals(entity, result)
    }

    @Test
    fun `cancel is idempotent when the booking is already CANCELLATION_PENDING`() {
        val entity = booking(status = BookingStatus.CANCELLATION_PENDING)
        whenever(bookingRepository.getReferenceById(entity.bookingId)).thenReturn(entity)
        whenever(bookingRepository.findByBookingId(entity.bookingId)).thenReturn(entity)

        val result = ops.cancel(entity.bookingId)

        assertEquals(entity, result)
    }

    @Test
    fun `cancel rethrows when the booking is in a state that never allowed cancellation`() {
        val entity = booking(status = BookingStatus.FAILED)
        whenever(bookingRepository.getReferenceById(entity.bookingId)).thenReturn(entity)
        whenever(bookingRepository.findByBookingId(entity.bookingId)).thenReturn(entity)

        assertFailsWith<IllegalStateTransitionException> { ops.cancel(entity.bookingId) }
    }

    @Test
    fun `cancel throws BookingNotFoundException when the row disappeared entirely`() {
        val entity = booking(status = BookingStatus.CANCELLED)
        whenever(bookingRepository.getReferenceById(entity.bookingId)).thenReturn(entity)
        whenever(bookingRepository.findByBookingId(entity.bookingId)).thenReturn(null)

        assertFailsWith<BookingNotFoundException> { ops.cancel(entity.bookingId) }
    }

    @Test
    fun `findOrInsertPending serializes the BookingCreatedPayload with the saved row's own id`() {
        whenever(bookingRepository.findByIdempotencyKey("bk-1")).thenReturn(null)
        val saved = booking(idempotencyKey = "bk-1", offerId = "off_9")
        whenever(bookingRepository.saveAndFlush(any<BookingEntity>())).thenReturn(saved)
        val payloadCaptor = argumentCaptor<Any>()

        ops.findOrInsertPending("bk-1", request(offerId = "off_9"))

        // writeValueAsString is called twice: once for requestPayload on the
        // entity being built, once for the BookingCreatedPayload passed to
        // enqueue() -- capture every invocation and confirm the payload one
        // carries the saved row's own bookingId, never the idempotency key.
        verify(objectMapper, times(2)).writeValueAsString(payloadCaptor.capture())
        val eventPayload = payloadCaptor.allValues.filterIsInstance<BookingCreatedPayload>().single()
        assertEquals(saved.bookingId, eventPayload.bookingId)
        assertEquals("bk-1", eventPayload.idempotencyKey)
        assertEquals("off_9", eventPayload.offerId)
    }
}
