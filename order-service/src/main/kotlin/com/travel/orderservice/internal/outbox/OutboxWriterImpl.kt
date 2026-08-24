package com.travel.orderservice.internal.outbox

import com.travel.orderservice.internal.persistence.OrderOutboxEntity
import com.travel.orderservice.internal.persistence.OrderOutboxRepository
import com.travel.orderservice.internal.service.OutboxEvent
import org.springframework.stereotype.Component
import kotlin.uuid.Uuid

@Component
class OutboxWriterImpl(private val repository: OrderOutboxRepository) : OutboxWriter {
    // No @Transactional here -- append() always runs as part of a caller's
    // own transaction (submit() in P4, advance() in P5). Adding one here
    // would start a *nested* transaction instead of joining the caller's,
    // defeating the whole "atomic with the state change" point of the
    // outbox pattern -- same reasoning as booking-service's
    // BookingEventOutboxWriterImpl.
    override fun append(aggregateId: Uuid, event: OutboxEvent) {
        repository.save(
            OrderOutboxEntity(aggregateId = aggregateId, eventType = event.eventType, payload = event.payload),
        )
    }
}
