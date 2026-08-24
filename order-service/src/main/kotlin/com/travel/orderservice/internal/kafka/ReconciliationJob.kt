package com.travel.orderservice.internal.kafka

import com.travel.duffel.api.booking.DuffelLookup
import com.travel.orderservice.api.OrderStatus
import com.travel.orderservice.api.OrderStatusEvent
import com.travel.orderservice.internal.persistence.OrderRepository
import com.travel.orderservice.internal.service.OrderMetrics
import com.travel.orderservice.internal.service.OrderTransitions
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

// The backstop for P6's "never retry a 202" rule -- something has to
// resolve AWAITING_AIRLINE_CONFIRMATION from the outside when Duffel's own
// webhook is late, lost, or never configured for this event type.
@Component
class ReconciliationJob(
    private val orders: OrderRepository,
    private val duffelLookup: DuffelLookup,
    private val transitions: OrderTransitions,
    private val metrics: OrderMetrics,
) {
    // Ten minutes, not 72 hours -- this sweep exists to catch a slow or
    // misconfigured webhook quickly, well inside Duffel's own retry budget,
    // not to replace it.
    @Scheduled(fixedDelay = 60_000)
    fun sweep() {
        val cutoff = Clock.System.now().minus(10.minutes)
        val stale = orders.findByStatusAndUpdatedAtBefore(OrderStatus.AWAITING_AIRLINE_CONFIRMATION, cutoff)

        stale.minOfOrNull { it.updatedAt }?.let { oldest ->
            metrics.recordAwaitingConfirmationAge((Clock.System.now() - oldest).inWholeSeconds)
        }

        for (order in stale) {
            // Looks up by our own metadata, not Duffel's order id -- a 202
            // doesn't always come back with one yet.
            val found = duffelLookup.findByMetadata("internal_order_id", order.id.toString())
            val event = found?.let { OrderStatusEvent.WEBHOOK_CONFIRMED } ?: OrderStatusEvent.RECONCILIATION_NOT_FOUND
            transitions.advance(order.id, event)
            metrics.reconciliationResolved(event.name)
        }
    }
}
