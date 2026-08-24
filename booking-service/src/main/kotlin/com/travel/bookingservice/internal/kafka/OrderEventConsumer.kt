package com.travel.bookingservice.internal.kafka

import com.travel.bookingservice.internal.service.BookingStatusEvent
import com.travel.bookingservice.internal.service.BookingTransactionalOps
import com.travel.bookingservice.internal.service.IllegalStateTransitionException
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.ObjectMapper
import kotlin.uuid.Uuid

@Component
class OrderEventConsumer(
    private val transactionalOps: BookingTransactionalOps,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(OrderEventConsumer::class.java)

    @KafkaListener(
        topics = ["order.events"],
        groupId = "booking-service",
        containerFactory = "bookingManualAckContainerFactory",
    )
    fun onOrderEvent(payload: String, ack: Acknowledgment) {
        // A message this consumer can't even parse -- a schema change on
        // order-service's side, or a stale/poison record from before a
        // producer-side fix shipped -- must never wedge the whole
        // partition retrying forever. Logged and acked, same tolerance as
        // an unrecognized eventType below, just one layer earlier.
        val event =
            try {
                objectMapper.readValue(payload, OrderEventEnvelope::class.java)
            } catch (ex: Exception) {
                log.warn("Skipping unparseable order.events message: {}", ex.message)
                ack.acknowledge()
                return
            }
        // event.bookingId here is order-service's own order id, which -- for
        // an order that originated from this saga -- was seeded from THIS
        // service's booking id at BookingCreated time (order_service.html
        // §P11 reuses the same key end to end).
        val statusEvent =
            when (event.eventType) {
                "OrderAwaitingConfirmation" -> BookingStatusEvent.PROVIDER_RESERVED
                "OrderConfirmed" -> BookingStatusEvent.PROVIDER_CONFIRMED
                "OrderFailed", "OrderPaymentFailed" -> BookingStatusEvent.PROVIDER_FAILED
                "OrderCancelled" -> BookingStatusEvent.CANCEL_CONFIRMED
                // OrderTicketed and any other post-CONFIRMED order event isn't a
                // booking-status change -- CONFIRMED is already this service's
                // terminal "succeeded" state.
                else -> null
            }
        if (statusEvent != null) {
            try {
                transactionalOps.advance(event.bookingId, statusEvent, event.failureReason)
            } catch (ex: IllegalStateTransitionException) {
                // A redelivered or out-of-order message applying an edge that no
                // longer matches the current state -- e.g. OrderConfirmed
                // arriving after this booking was already independently
                // cancelled. Logged, not thrown: the booking's own status
                // is still correct, just not what this one message wanted.
            }
        }
        ack.acknowledge()
    }
}

// The minimal shape this consumer actually needs -- not order-service's
// full outbox payload. The wire key is "orderId" (order-service's own
// domain name for the field); this side maps it onto "bookingId" because
// order-service's order id was seeded from booking-service's booking id at
// creation time (see the step-note above), so it IS this service's own
// booking id for any order that came through this saga. failureReason is
// only ever populated on the two failure edges, and null otherwise.
data class OrderEventEnvelope(
    @JsonProperty("orderId") val bookingId: Uuid,
    val eventType: String,
    val failureReason: String? = null,
)
