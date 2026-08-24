package com.travel.orderservice.internal.service

import com.travel.orderservice.api.OrderStatus
import com.travel.orderservice.api.OrderStatusEvent
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrderMetricsTest {
    private val registry = SimpleMeterRegistry()
    private val metrics = OrderMetrics(registry)

    @Test
    fun `duplicatePrevented tags order_duplicate_prevented_total by layer`() {
        metrics.duplicatePrevented("unique_constraint")
        metrics.duplicatePrevented("unique_constraint")
        metrics.duplicatePrevented("unique_constraint_race")

        assertEquals(2.0, registry.counter("order_duplicate_prevented_total", "layer", "unique_constraint").count())
        assertEquals(1.0, registry.counter("order_duplicate_prevented_total", "layer", "unique_constraint_race").count())
    }

    @Test
    fun `stateTransition tags order_state_transition_total by from and to`() {
        metrics.stateTransition(OrderStatus.PENDING_SUBMISSION, OrderStatus.CONFIRMED)

        assertEquals(
            1.0,
            registry.counter("order_state_transition_total", "from", "PENDING_SUBMISSION", "to", "CONFIRMED").count(),
        )
    }

    @Test
    fun `reconciliationResolved tags order_reconciliation_resolved_total by resolution`() {
        metrics.reconciliationResolved("WEBHOOK_CONFIRMED")

        assertEquals(
            1.0,
            registry.counter("order_reconciliation_resolved_total", "resolution", "WEBHOOK_CONFIRMED").count(),
        )
    }

    @Test
    fun `optimisticLockRetry tags order_optimistic_lock_retry_total by event`() {
        metrics.optimisticLockRetry(OrderStatusEvent.DUFFEL_2XX_FULL)

        assertEquals(
            1.0,
            registry.counter("order_optimistic_lock_retry_total", "event", "DUFFEL_2XX_FULL").count(),
        )
    }

    @Test
    fun `timeDuffelCall returns the block's result and records the duration`() {
        val result = metrics.timeDuffelCall { "booked" }

        assertEquals("booked", result)
        assertEquals(1, registry.timer("order_duffel_call_duration_seconds").count())
    }

    @Test
    fun `timeDuffelCall still records the duration when the block throws`() {
        assertFailsWith<IllegalStateException> {
            metrics.timeDuffelCall { throw IllegalStateException("boom") }
        }

        assertEquals(1, registry.timer("order_duffel_call_duration_seconds").count())
    }

    @Test
    fun `recordAwaitingConfirmationAge updates the gauge value`() {
        metrics.recordAwaitingConfirmationAge(42)

        assertEquals(42.0, registry.get("order_awaiting_confirmation_age_seconds").gauge().value())
    }

    @Test
    fun `recordOutboxBacklog updates the gauge value`() {
        metrics.recordOutboxBacklog(7)

        assertEquals(7.0, registry.get("order_outbox_backlog").gauge().value())
    }

    @Test
    fun `publishFailure tags order_outbox_publish_failure_total by event type`() {
        metrics.publishFailure("OrderConfirmed")

        assertEquals(
            1.0,
            registry.counter("order_outbox_publish_failure_total", "event_type", "OrderConfirmed").count(),
        )
    }
}
