package com.travel.orderservice.internal.service

import com.travel.orderservice.api.IllegalStateTransitionException
import com.travel.orderservice.api.OrderStatus
import com.travel.orderservice.api.OrderStatusEvent
import com.travel.orderservice.internal.outbox.OutboxWriter
import com.travel.orderservice.internal.persistence.OrderEntity
import com.travel.orderservice.internal.persistence.OrderRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OrderTransitionsTest {
    private val orders: OrderRepository = mock()
    private val outbox: OutboxWriter = mock()
    private val metrics: OrderMetrics = mock()
    private val transitions = OrderTransitions(orders, outbox, metrics)

    @Test
    fun `advance transitions the order, stamps updatedAt, appends an outbox event, and records the transition metric`() {
        val order = OrderEntity(idempotencyKey = "idem-1", offerId = "off_1", status = OrderStatus.PENDING_SUBMISSION)
        whenever(orders.getReferenceById(order.id)).thenReturn(order)
        val before = order.updatedAt

        val view = transitions.advance(order.id, OrderStatusEvent.DUFFEL_2XX_FULL)

        assertEquals(OrderStatus.CONFIRMED, order.status)
        assertEquals(OrderStatus.CONFIRMED, view.status)
        assertTrue(order.updatedAt >= before)

        val outboxCaptor = argumentCaptor<OutboxEvent>()
        verify(outbox).append(eq(order.id), outboxCaptor.capture())
        assertEquals("OrderConfirmed", outboxCaptor.firstValue.eventType)
        verify(metrics).stateTransition(OrderStatus.PENDING_SUBMISSION, OrderStatus.CONFIRMED)
    }

    @Test
    fun `advance throws on an illegal edge and never touches the outbox or the metric`() {
        val order = OrderEntity(idempotencyKey = "idem-1", offerId = "off_1", status = OrderStatus.CANCELLED)
        whenever(orders.getReferenceById(order.id)).thenReturn(order)

        assertFailsWith<IllegalStateTransitionException> {
            transitions.advance(order.id, OrderStatusEvent.SUBMIT)
        }

        verify(outbox, never()).append(any(), any())
        verify(metrics, never()).stateTransition(any(), any())
    }

    @Test
    fun `advance looks the order up by the id it was given`() {
        val order = OrderEntity(idempotencyKey = "idem-1", offerId = "off_1", status = OrderStatus.CONFIRMED)
        whenever(orders.getReferenceById(order.id)).thenReturn(order)

        transitions.advance(order.id, OrderStatusEvent.TICKETED)

        verify(orders).getReferenceById(order.id)
    }
}
