package com.travel.orderservice.internal.kafka

import com.travel.duffel.api.booking.DuffelLookup
import com.travel.orderservice.api.OrderStatus
import com.travel.orderservice.api.OrderStatusEvent
import com.travel.orderservice.internal.persistence.OrderEntity
import com.travel.orderservice.internal.persistence.OrderRepository
import com.travel.orderservice.internal.service.OrderMetrics
import com.travel.orderservice.internal.service.OrderTransitions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class ReconciliationJobTest {
    private val orders: OrderRepository = mock()
    private val duffelLookup: DuffelLookup = mock()
    private val transitions: OrderTransitions = mock()
    private val metrics: OrderMetrics = mock()
    private val job = ReconciliationJob(orders, duffelLookup, transitions, metrics)

    private fun staleOrder(updatedAt: kotlin.time.Instant = Clock.System.now().minus(20.minutes)) =
        OrderEntity(
            idempotencyKey = "idem-${kotlin.uuid.Uuid.random()}",
            offerId = "off_1",
            status = OrderStatus.AWAITING_AIRLINE_CONFIRMATION,
            updatedAt = updatedAt,
        )

    @Test
    fun `sweep queries only AWAITING_AIRLINE_CONFIRMATION with a cutoff in the past, and does nothing when nothing is stale`() {
        whenever(orders.findByStatusAndUpdatedAtBefore(any(), any())).thenReturn(emptyList())

        job.sweep()

        val statusCaptor = argumentCaptor<OrderStatus>()
        val cutoffCaptor = argumentCaptor<kotlin.time.Instant>()
        verify(orders).findByStatusAndUpdatedAtBefore(statusCaptor.capture(), cutoffCaptor.capture())
        assertTrue(statusCaptor.firstValue == OrderStatus.AWAITING_AIRLINE_CONFIRMATION)
        assertTrue(cutoffCaptor.firstValue < Clock.System.now())
        verify(metrics, never()).recordAwaitingConfirmationAge(any())
        verify(duffelLookup, never()).findByMetadata(any(), any())
    }

    @Test
    fun `sweep resolves to WEBHOOK_CONFIRMED when Duffel already has a matching order`() {
        val order = staleOrder()
        whenever(orders.findByStatusAndUpdatedAtBefore(any(), any())).thenReturn(listOf(order))
        whenever(duffelLookup.findByMetadata("internal_order_id", order.id.toString())).thenReturn("duf_order_1")

        job.sweep()

        verify(transitions).advance(order.id, OrderStatusEvent.WEBHOOK_CONFIRMED)
        verify(metrics).reconciliationResolved("WEBHOOK_CONFIRMED")
    }

    @Test
    fun `sweep resolves to RECONCILIATION_NOT_FOUND when Duffel has no matching order`() {
        val order = staleOrder()
        whenever(orders.findByStatusAndUpdatedAtBefore(any(), any())).thenReturn(listOf(order))
        whenever(duffelLookup.findByMetadata("internal_order_id", order.id.toString())).thenReturn(null)

        job.sweep()

        verify(transitions).advance(order.id, OrderStatusEvent.RECONCILIATION_NOT_FOUND)
        verify(metrics).reconciliationResolved("RECONCILIATION_NOT_FOUND")
    }

    @Test
    fun `sweep records the age of only the single oldest stale order`() {
        val oldest = staleOrder(updatedAt = Clock.System.now().minus(20.minutes))
        val newer = staleOrder(updatedAt = Clock.System.now().minus(5.minutes))
        whenever(orders.findByStatusAndUpdatedAtBefore(any(), any())).thenReturn(listOf(newer, oldest))
        whenever(duffelLookup.findByMetadata(any(), any())).thenReturn(null)

        job.sweep()

        val ageCaptor = argumentCaptor<Long>()
        verify(metrics).recordAwaitingConfirmationAge(ageCaptor.capture())
        // ~20 minutes = 1200s -- generous tolerance around real test-clock drift.
        assertTrue(ageCaptor.firstValue in 1150L..1260L, "expected ~1200s, was ${ageCaptor.firstValue}")
    }

    @Test
    fun `sweep advances every stale order, not just the first`() {
        val first = staleOrder()
        val second = staleOrder()
        whenever(orders.findByStatusAndUpdatedAtBefore(any(), any())).thenReturn(listOf(first, second))
        whenever(duffelLookup.findByMetadata(any(), any())).thenReturn("duf_order_1")

        job.sweep()

        verify(transitions).advance(first.id, OrderStatusEvent.WEBHOOK_CONFIRMED)
        verify(transitions).advance(second.id, OrderStatusEvent.WEBHOOK_CONFIRMED)
    }
}
