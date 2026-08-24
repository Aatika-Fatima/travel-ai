package com.travel.orderservice.internal.service

import com.travel.orderservice.api.OrderStatus
import com.travel.orderservice.api.OrderStatusEvent
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class OrderMetrics(private val registry: MeterRegistry) {

    // Gauges are pull-based, not imperative — Micrometer samples whatever
    // these two holders currently contain whenever a scrape happens, so
    // there's no "increment" call for either. record*() below just updates
    // the holder; the gauge itself is wired once, here, at construction.
    private val awaitingConfirmationAge = AtomicLong(0)
    private val outboxBacklog = AtomicLong(0)

    init {
        registry.gauge("order_awaiting_confirmation_age_seconds", awaitingConfirmationAge)
        registry.gauge("order_outbox_backlog", outboxBacklog)
    }

    fun duplicatePrevented(layer: String) =
        registry.counter("order_duplicate_prevented_total", "layer", layer).increment()

    fun stateTransition(from: OrderStatus, to: OrderStatus) =
        registry.counter("order_state_transition_total", "from", from.name, "to", to.name).increment()

    fun reconciliationResolved(resolution: String) =
        registry.counter("order_reconciliation_resolved_total", "resolution", resolution).increment()

    // advance() (P5) is @Retryable on ObjectOptimisticLockingFailureException
    // — this is called once per retry attempt, not once per advance(), so
    // a transition that's a genuine contention hot spot shows up as a
    // climbing rate here well before it shows up as user-visible latency.
    fun optimisticLockRetry(event: OrderStatusEvent) =
        registry.counter("order_optimistic_lock_retry_total", "event", event.name).increment()

    // Wraps gateway.createBooking() in callDuffelAndAdvance() (P4) — timed
    // whether the call succeeds or throws, since a 503 that's slow to fail
    // eats into the retry budget just as much as a slow success does.
    fun <T> timeDuffelCall(block: () -> T): T {
        val sample = Timer.start(registry)
        try {
            return block()
        } finally {
            sample.stop(registry.timer("order_duffel_call_duration_seconds"))
        }
    }

    // Called once per tick from ReconciliationJob.sweep() (P8) with the age
    // of the single oldest AWAITING_AIRLINE_CONFIRMATION row it just read —
    // the worst case is what matters for "are we approaching 72h," not an
    // average across every row.
    fun recordAwaitingConfirmationAge(seconds: Long) = awaitingConfirmationAge.set(seconds)

    // Called once per tick from OutboxRelay.relay() (P7) with the count of
    // rows still unpublished after that tick's batch — a number that's
    // consistently non-zero (not just non-zero on a single sample) is what
    // "is the relay keeping up" actually means.
    fun recordOutboxBacklog(count: Long) = outboxBacklog.set(count)

    // Called from OutboxRelay.relay() (P7) when a Kafka send for one row
    // fails to complete -- the row stays unpublished and is picked up again
    // on the relay's next tick, so this is purely observability, not the
    // retry mechanism itself.
    fun publishFailure(eventType: String) =
        registry.counter("order_outbox_publish_failure_total", "event_type", eventType).increment()
}