package com.travel.orderservice.internal.kafka

import com.travel.orderservice.internal.persistence.OrderRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.time.Clock
import kotlin.uuid.Uuid

enum class CustomerPaymentStatus { AWAITING_PAYMENT, PAID }

@Component
class PaymentEventConsumer(private val orders: OrderRepository) {
    private val mapper = jacksonObjectMapper()

    @KafkaListener(
        topics = ["payment.events"],
        groupId = "order-service",
        containerFactory = "orderManualAckContainerFactory",
    )
    // "bookingId" in this payload is payment-service's field name for what
    // is, in this saga, order-service's own order id -- see the boundary
    // note in booking_service.html §Boundary notes for why the name
    // doesn't mean what it says at first glance.
    //
    // @Transactional / @Retryable live here, on the proxied listener entry
    // point, not on markCaptured: markCaptured is a plain in-class call, so
    // annotating it did nothing (self-invocation bypasses the proxy) and
    // markCaptured ran with no session -- getReferenceById's lazy proxy
    // then threw LazyInitializationException on the first field access.
    @Retryable(retryFor = [ObjectOptimisticLockingFailureException::class], maxAttempts = 4, backoff = Backoff(delay = 50, multiplier = 2.0))
    @Transactional
    fun onPaymentEvent(payload: String, ack: Acknowledgment) {
        val event = mapper.readValue(payload, PaymentEventPayload::class.java)
        if (event.eventType == "payment.captured") {
            markCaptured(Uuid.parse(event.bookingId))
        }
        // event.eventType == "payment.refunded" is out of scope for this
        // phase -- a captured-then-refunded order is a cancellation-style
        // flow this plan doesn't model yet.
        ack.acknowledge()
    }

    fun markCaptured(orderId: Uuid) {
        val order = orders.getReferenceById(orderId)
        // Redelivery-safe by construction: applying PAID on top of PAID is a
        // deliberate no-op, the same idempotent-transition principle P5
        // already uses for OrderStatus, just without needing a full
        // allow-list for a two-value field.
        if (order.customerPaymentStatus == CustomerPaymentStatus.PAID) return
        order.customerPaymentStatus = CustomerPaymentStatus.PAID
        order.updatedAt = Clock.System.now()
    }
}
