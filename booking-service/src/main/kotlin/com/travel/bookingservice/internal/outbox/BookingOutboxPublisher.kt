package com.travel.bookingservice.internal.outbox

import com.travel.bookingservice.internal.persistence.BookingEventOutboxRepository
import com.travel.bookingservice.internal.service.BookingStatusEvent
import com.travel.bookingservice.internal.service.BookingTransactionalOps
import com.travel.bookingservice.internal.service.IllegalStateTransitionException
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock

@Component
class BookingOutboxPublisher(
    private val repository: BookingEventOutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val transactionalOps: BookingTransactionalOps,
) {
    @Transactional
    @Scheduled(fixedDelay = 1_000)
    fun publishPending() {
        repository.claimPending().forEach { row ->
            // bookingId as the Kafka key -- every event for one booking lands on
            // the same partition, so order-service's consumer (order_service.html
            // §P11) sees them in write order.
            kafkaTemplate.send("booking.events", row.bookingId.toString(), row.payload)
            row.status = "PUBLISHED"
            row.publishedAt = Clock.System.now()

            // Only a successfully published BookingCreated moves the booking
            // itself to IN_PROGRESS (P4) -- BookingCancellationRequested
            // doesn't correspond to a status edge of its own, since cancel()
            // already applied CANCEL_REQUESTED synchronously when it ran.
            if (row.eventType == "BookingCreated") {
                try {
                    transactionalOps.advance(row.bookingId, BookingStatusEvent.PUBLISHED)
                } catch (ex: IllegalStateTransitionException) {
                    // The booking moved on already -- e.g. cancelled between the
                    // insert and this tick, see P4's worked example. The
                    // event still reached Kafka; there's nothing to retry.
                }
            }
        }
    }
}