package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.persistence.BookingEntity
import com.travel.bookingservice.internal.persistence.BookingRepository
import com.travel.bookingservice.internal.persistence.BookingStatus
import com.travel.common.model.BookingRequest
import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class BookingServiceImplTest {
    private val transactionalOps: BookingTransactionalOps = mock()
    private val bookingRepository: BookingRepository = mock()
    private val metrics: BookingMetrics = mock()
    private val service = BookingServiceImpl(transactionalOps, bookingRepository, metrics)

    private fun request() =
        BookingRequest(
            offerId = "off_1",
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

    private fun booking(status: BookingStatus = BookingStatus.INITIATED) =
        BookingEntity(idempotencyKey = "bk-1", offerId = "off_1", status = status, requestPayload = "{}")

    @Test
    fun `submit marks the view justCreated when the row is brand new`() {
        val saved = booking()
        whenever(transactionalOps.findOrInsertPending("bk-1", request())).thenReturn(saved to false)

        val view = service.submit("bk-1", request())

        assertEquals(saved.bookingId, view.bookingId)
        assertTrue(view.justCreated)
        verify(metrics, never()).duplicatePrevented(any())
    }

    @Test
    fun `submit marks the view not-justCreated and records duplicatePrevented when the row already existed`() {
        val existing = booking()
        whenever(transactionalOps.findOrInsertPending("bk-1", request())).thenReturn(existing to true)

        val view = service.submit("bk-1", request())

        assertFalse(view.justCreated)
        verify(metrics).duplicatePrevented("unique_constraint_or_lock")
    }

    @Test
    fun `get returns the view for a known booking id`() {
        val entity = booking()
        whenever(bookingRepository.findByBookingId(entity.bookingId)).thenReturn(entity)

        val view = service.get(entity.bookingId)

        assertEquals(entity.bookingId, view.bookingId)
        assertEquals(entity.status, view.status)
    }

    @Test
    fun `get throws BookingNotFoundException for an unknown booking id`() {
        val id = Uuid.random()
        whenever(bookingRepository.findByBookingId(id)).thenReturn(null)

        assertFailsWith<BookingNotFoundException> { service.get(id) }
    }

    @Test
    fun `cancel delegates to transactionalOps and returns its view`() {
        val cancelled = booking(status = BookingStatus.CANCELLED)
        whenever(transactionalOps.cancel(cancelled.bookingId)).thenReturn(cancelled)

        val view = service.cancel(cancelled.bookingId)

        assertEquals(BookingStatus.CANCELLED, view.status)
    }
}
