package com.travel.bookingservice.internal.kafka

import com.travel.bookingservice.internal.persistence.BookingEntity
import com.travel.bookingservice.internal.persistence.BookingRepository
import com.travel.bookingservice.internal.persistence.BookingStatus
import com.travel.bookingservice.internal.service.BookingMetrics
import com.travel.bookingservice.internal.service.BookingStatusEvent
import com.travel.bookingservice.internal.service.BookingTransactionalOps
import com.travel.bookingservice.internal.service.IllegalStateTransitionException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Clock
import kotlin.uuid.Uuid

class BookingExpirySweepTest {
    private val bookings: BookingRepository = mock()
    private val transactionalOps: BookingTransactionalOps = mock()
    private val metrics: BookingMetrics = mock()
    private val sweep = BookingExpirySweep(bookings, transactionalOps, metrics)

    private fun booking(status: BookingStatus) =
        BookingEntity(idempotencyKey = "bk-${Uuid.random()}", offerId = "off_1", status = status, requestPayload = "{}")

    @Test
    fun `sweep queries only the statuses that can meaningfully expire, with a cutoff in the past`() {
        whenever(bookings.findStaleActive(any(), any())).thenReturn(emptyList())

        sweep.sweep()

        val statusesCaptor = argumentCaptor<Set<BookingStatus>>()
        val cutoffCaptor = argumentCaptor<kotlin.time.Instant>()
        verify(bookings).findStaleActive(statusesCaptor.capture(), cutoffCaptor.capture())
        assert(
            statusesCaptor.firstValue ==
                setOf(BookingStatus.INITIATED, BookingStatus.IN_PROGRESS, BookingStatus.RESERVED),
        )
        assert(cutoffCaptor.firstValue < Clock.System.now())
    }

    @Test
    fun `sweep advances every stale booking to EXPIRED and records a metric per success`() {
        val a = booking(BookingStatus.INITIATED)
        val b = booking(BookingStatus.IN_PROGRESS)
        whenever(bookings.findStaleActive(any(), any())).thenReturn(listOf(a, b))

        sweep.sweep()

        verify(transactionalOps).advance(a.bookingId, BookingStatusEvent.EXPIRED)
        verify(transactionalOps).advance(b.bookingId, BookingStatusEvent.EXPIRED)
        verify(metrics, times(2)).expired()
    }

    @Test
    fun `sweep swallows a race where the booking already resolved between the query and the transition`() {
        val a = booking(BookingStatus.RESERVED)
        whenever(bookings.findStaleActive(any(), any())).thenReturn(listOf(a))
        whenever(transactionalOps.advance(a.bookingId, BookingStatusEvent.EXPIRED))
            .thenThrow(IllegalStateTransitionException(BookingStatus.CANCELLED, BookingStatusEvent.EXPIRED))

        sweep.sweep()

        verify(metrics, never()).expired()
    }

    @Test
    fun `sweep only counts metrics for rows that actually transitioned when mixed with a failure`() {
        val resolved = booking(BookingStatus.INITIATED)
        val stillStale = booking(BookingStatus.IN_PROGRESS)
        whenever(bookings.findStaleActive(any(), any())).thenReturn(listOf(resolved, stillStale))
        whenever(transactionalOps.advance(resolved.bookingId, BookingStatusEvent.EXPIRED))
            .thenThrow(IllegalStateTransitionException(BookingStatus.CANCELLED, BookingStatusEvent.EXPIRED))
        whenever(transactionalOps.advance(stillStale.bookingId, BookingStatusEvent.EXPIRED)).thenReturn(stillStale)

        sweep.sweep()

        verify(metrics, times(1)).expired()
    }
}
