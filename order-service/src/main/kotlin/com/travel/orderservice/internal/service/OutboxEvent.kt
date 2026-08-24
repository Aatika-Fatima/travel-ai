package com.travel.orderservice.internal.service

import com.travel.orderservice.api.OrderStatusEvent
import com.travel.orderservice.internal.persistence.OrderEntity
import kotlin.uuid.Uuid

class OutboxEvent(val eventType:String, val payload:String)

object OrderEvent {
    // Written once, synchronously with the initial insert in submit() —
    // before Duffel has even been called — so a crash between the insert
    // and the Duffel call still leaves a durable "this order exists and is
    // pending" event for downstream consumers to see.
    fun pending(orderId: Uuid): OutboxEvent = OutboxEvent(
        eventType = "OrderPending",
        payload = """{"orderId":"$orderId","eventType":"OrderPending"}""",
    )
}

// One entry per OrderStatusEvent, deliberately exhaustive (no else branch)
// so a ninth event added to that enum fails to compile here until it's
// given an event_type — the same "no silent gap" property ALLOWED (P5)
// already has.
fun OrderStatusEvent.toOutboxPayload(order: OrderEntity): OutboxEvent {
    val eventType = when (this) {
        OrderStatusEvent.SUBMIT -> "OrderSubmitted"
        OrderStatusEvent.DUFFEL_2XX_FULL, OrderStatusEvent.WEBHOOK_CONFIRMED -> "OrderConfirmed"
        OrderStatusEvent.DUFFEL_PENDING -> "OrderAwaitingConfirmation"
        OrderStatusEvent.DUFFEL_PAYMENT_DECLINED -> "OrderPaymentFailed"
        OrderStatusEvent.DUFFEL_VALIDATION_FAILED, OrderStatusEvent.RECONCILIATION_NOT_FOUND -> "OrderFailed"
        OrderStatusEvent.TICKETED -> "OrderTicketed"
        OrderStatusEvent.CANCEL -> "OrderCancelled"
    }
    // eventType has to be folded into the payload body itself, not left as
    // only the outbox row's own DB column -- OutboxRelay.relay() sends just
    // row.payload to Kafka, so a consumer (booking-service's
    // OrderEventConsumer) has no other way to learn which transition this
    // was. Same fix docs/razor_pay.html's own P12 prerequisite callout
    // flags for payment-service's identical gap on payment.events.
    return OutboxEvent(
        eventType = eventType,
        payload = """{"orderId":"${order.id}","status":"${order.status}","eventType":"$eventType"}""",
    )
}