package com.travel.orderservice.internal.service

import com.travel.orderservice.api.OrderStatusEvent
import com.travel.orderservice.api.OrderView
import com.travel.orderservice.internal.outbox.OutboxWriter
import com.travel.orderservice.internal.persistence.OrderRepository
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock
import kotlin.uuid.Uuid

// Deliberately a separate bean from OrderServiceImpl -- @Retryable only
// works through the Spring AOP proxy, and OrderServiceImpl calling this as
// a private method on itself would bypass that proxy and silently disable
// the retry.
@Component
class OrderTransitions(
    private val orders: OrderRepository,
    private val outbox: OutboxWriter,
    private val metrics: OrderMetrics,
) {
    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 4,
        backoff = Backoff(delay = 50, multiplier = 2.0),
    )
    @Transactional
    fun advance(orderId: Uuid, event: OrderStatusEvent): OrderView {
        val order = orders.getReferenceById(orderId)
        val from = order.status
        order.status = order.status.transition(event) // throws on an illegal edge
        order.updatedAt = Clock.System.now()
        outbox.append(orderId, event.toOutboxPayload(order))
        metrics.stateTransition(from, order.status)
        return order.toView()
    }
}
