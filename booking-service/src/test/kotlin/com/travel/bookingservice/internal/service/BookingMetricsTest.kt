package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.persistence.BookingStatus
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BookingMetricsTest {
    private val registry = SimpleMeterRegistry()
    private val metrics = BookingMetrics(registry)

    @Test
    fun `created increments booking_created_total`() {
        metrics.created()
        metrics.created()

        assertEquals(2.0, registry.counter("booking_created_total").count())
    }

    @Test
    fun `duplicatePrevented tags booking_duplicate_prevented_total by reason`() {
        metrics.duplicatePrevented("unique_constraint_or_lock")
        metrics.duplicatePrevented("unique_constraint_or_lock")
        metrics.duplicatePrevented("other_reason")

        assertEquals(
            2.0,
            registry.counter("booking_duplicate_prevented_total", "reason", "unique_constraint_or_lock").count(),
        )
        assertEquals(1.0, registry.counter("booking_duplicate_prevented_total", "reason", "other_reason").count())
    }

    @Test
    fun `optimisticLockConflict increments booking_optimistic_lock_conflict_total`() {
        metrics.optimisticLockConflict()

        assertEquals(1.0, registry.counter("booking_optimistic_lock_conflict_total").count())
    }

    @Test
    fun `transitioned tags booking_status_total by the target status`() {
        metrics.transitioned(BookingStatus.CONFIRMED)
        metrics.transitioned(BookingStatus.CONFIRMED)
        metrics.transitioned(BookingStatus.CANCELLED)

        assertEquals(2.0, registry.counter("booking_status_total", "status", "CONFIRMED").count())
        assertEquals(1.0, registry.counter("booking_status_total", "status", "CANCELLED").count())
    }

    @Test
    fun `expired is transitioned tagged EXPIRED, not a separate counter`() {
        metrics.expired()

        assertEquals(1.0, registry.counter("booking_status_total", "status", "EXPIRED").count())
    }
}
