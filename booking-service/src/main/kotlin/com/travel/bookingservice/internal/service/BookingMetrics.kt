package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.persistence.BookingStatus
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class BookingMetrics(private val meterRegistry: MeterRegistry) {
    fun created() = meterRegistry.counter("booking_created_total").increment()
    fun duplicatePrevented(reason: String) =
        meterRegistry.counter("booking_duplicate_prevented_total", "reason", reason).increment()
    fun optimisticLockConflict() = meterRegistry.counter("booking_optimistic_lock_conflict_total").increment()
    // One counter, tagged by the resulting status -- covers every P4/P9/P10
    // transition without a new method per edge as the state machine grows.
    fun transitioned(to: BookingStatus) = meterRegistry.counter("booking_status_total", "status", to.name).increment()
    fun expired() = transitioned(BookingStatus.EXPIRED)
}
