package com.travel.bookingservice.internal.kafka

import com.travel.bookingservice.internal.persistence.BookingRepository
import com.travel.bookingservice.internal.persistence.BookingStatus
import com.travel.bookingservice.internal.service.BookingMetrics
import com.travel.bookingservice.internal.service.BookingStatusEvent
import com.travel.bookingservice.internal.service.BookingTransactionalOps
import com.travel.bookingservice.internal.service.IllegalStateTransitionException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Component
class BookingExpirySweep(
    private val bookings: BookingRepository,
    private val transactionalOps: BookingTransactionalOps,
    private val metrics: BookingMetrics,
) {
    // Every 60s is deliberately coarse -- this is a safety net for a saga
    // that's already stuck, not a latency-sensitive path. See P6 for why a
    // separate scheduled thread here is safe by the same reasoning as
    // P5's relay.
    @Scheduled(fixedDelay = 60_000)
    fun sweep() {
        val cutoff = Clock.System.now().minus(EXPIRY_WINDOW)
        // CONFIRMED, CANCELLATION_PENDING and the three terminal states are
        // deliberately excluded -- see P1's findStaleActive for the query
        // itself; only INITIATED / IN_PROGRESS / RESERVED can meaningfully
        // "die of old age" per the ALLOWED map's EXPIRE edges (P4).
        val staleStatuses = setOf(BookingStatus.INITIATED, BookingStatus.IN_PROGRESS, BookingStatus.RESERVED)
        bookings.findStaleActive(staleStatuses, cutoff).forEach { booking ->
            try {
                transactionalOps.advance(booking.bookingId, BookingStatusEvent.EXPIRED)
                metrics.expired()
            } catch (ex: IllegalStateTransitionException) {
                // Resolved between the query above and this call -- P9 or a
                // cancel() got there first. Nothing to do.
            }
        }
    }

    companion object {
        private val EXPIRY_WINDOW = 20.minutes
    }
}