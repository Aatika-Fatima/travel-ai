package com.travel.orderservice.internal.kafka

import com.travel.orderservice.internal.persistence.OrderEntity
import com.travel.orderservice.internal.persistence.OrderRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.kafka.support.Acknowledgment
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

class PaymentEventConsumerTest {
    private val orders: OrderRepository = mock()
    private val consumer = PaymentEventConsumer(orders)
    private val ack: Acknowledgment = mock()

    @Test
    fun `onPaymentEvent marks the order captured and acknowledges for a payment-captured event`() {
        val orderId = Uuid.random()
        val order = OrderEntity(idempotencyKey = "idem-1", offerId = "off_1")
        whenever(orders.getReferenceById(orderId)).thenReturn(order)
        val payload = """{"event":"payment.captured","bookingId":"$orderId","paymentId":"pay_1"}"""

        consumer.onPaymentEvent(payload, ack)

        assertEquals(CustomerPaymentStatus.PAID, order.customerPaymentStatus)
        verify(ack).acknowledge()
    }

    @Test
    fun `onPaymentEvent leaves the order untouched but still acknowledges for a payment-refunded event`() {
        val orderId = Uuid.random()
        val payload = """{"event":"payment.refunded","bookingId":"$orderId","paymentId":"pay_1"}"""

        consumer.onPaymentEvent(payload, ack)

        verifyNoInteractions(orders)
        verify(ack).acknowledge()
    }

    @Test
    fun `markCaptured is a no-op, including no updatedAt bump, when the order is already PAID`() {
        val orderId = Uuid.random()
        val fixedUpdatedAt = Instant.fromEpochMilliseconds(1_000)
        val order =
            OrderEntity(
                idempotencyKey = "idem-1",
                offerId = "off_1",
                customerPaymentStatus = CustomerPaymentStatus.PAID,
                updatedAt = fixedUpdatedAt,
            )
        whenever(orders.getReferenceById(orderId)).thenReturn(order)

        consumer.markCaptured(orderId)

        assertEquals(CustomerPaymentStatus.PAID, order.customerPaymentStatus)
        assertEquals(fixedUpdatedAt, order.updatedAt)
    }

    @Test
    fun `markCaptured transitions an AWAITING_PAYMENT order to PAID and bumps updatedAt`() {
        val orderId = Uuid.random()
        val fixedUpdatedAt = Instant.fromEpochMilliseconds(1_000)
        val order =
            OrderEntity(
                idempotencyKey = "idem-1",
                offerId = "off_1",
                customerPaymentStatus = CustomerPaymentStatus.AWAITING_PAYMENT,
                updatedAt = fixedUpdatedAt,
            )
        whenever(orders.getReferenceById(orderId)).thenReturn(order)

        consumer.markCaptured(orderId)

        assertEquals(CustomerPaymentStatus.PAID, order.customerPaymentStatus)
        assertEquals(true, order.updatedAt > fixedUpdatedAt)
    }
}
