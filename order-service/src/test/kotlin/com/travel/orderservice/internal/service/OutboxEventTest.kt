package com.travel.orderservice.internal.service

import com.travel.orderservice.api.OrderStatusEvent
import com.travel.orderservice.internal.persistence.OrderEntity
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class OutboxEventTest {
    private val order = OrderEntity(idempotencyKey = "idem-1", offerId = "off_1")

    @Test
    fun `OrderEvent pending carries the order id in an OrderPending payload`() {
        val orderId = Uuid.random()

        val event = OrderEvent.pending(orderId)

        assertEquals("OrderPending", event.eventType)
        assertEquals("""{"orderId":"$orderId","eventType":"OrderPending"}""", event.payload)
    }

    // One entry per OrderStatusEvent -- mirrors the exhaustive `when` in
    // toOutboxPayload() itself, so a ninth event added there without a
    // corresponding row here is caught by this test, not just left silently
    // untested.
    @Test
    fun `toOutboxPayload maps every OrderStatusEvent to exactly the event type the outbox row should carry`() {
        val expected =
            mapOf(
                OrderStatusEvent.SUBMIT to "OrderSubmitted",
                OrderStatusEvent.DUFFEL_2XX_FULL to "OrderConfirmed",
                OrderStatusEvent.DUFFEL_PENDING to "OrderAwaitingConfirmation",
                OrderStatusEvent.DUFFEL_PAYMENT_DECLINED to "OrderPaymentFailed",
                OrderStatusEvent.DUFFEL_VALIDATION_FAILED to "OrderFailed",
                OrderStatusEvent.WEBHOOK_CONFIRMED to "OrderConfirmed",
                OrderStatusEvent.RECONCILIATION_NOT_FOUND to "OrderFailed",
                OrderStatusEvent.TICKETED to "OrderTicketed",
                OrderStatusEvent.CANCEL to "OrderCancelled",
            )

        assertEquals(OrderStatusEvent.entries.toSet(), expected.keys, "every OrderStatusEvent is covered by this test")
        for ((event, eventType) in expected) {
            assertEquals(eventType, event.toOutboxPayload(order).eventType, "event=$event")
        }
    }

    @Test
    fun `toOutboxPayload carries the order id, current status, and event type in the payload`() {
        val payload = OrderStatusEvent.DUFFEL_2XX_FULL.toOutboxPayload(order).payload

        assertEquals(
            """{"orderId":"${order.id}","status":"${order.status}","eventType":"OrderConfirmed"}""",
            payload,
        )
    }
}
