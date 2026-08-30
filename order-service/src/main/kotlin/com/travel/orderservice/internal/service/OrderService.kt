package com.travel.orderservice.internal.service

import com.travel.orderservice.api.OrderView
import com.travel.orderservice.api.SubmitOrderCommand
import com.travel.orderservice.internal.persistence.OrderEntity
import kotlin.uuid.Uuid

interface OrderService {
    fun submit(command: SubmitOrderCommand): OrderView

    fun findById(orderId: Uuid): OrderView?

    // The client-supplied Idempotency-Key from POST /bookings (booking-
    // service) and POST /orders both end up as orders.idempotency_key
    // unchanged -- see BookingSagaConsumer's step-note. That means a caller
    // who only ever knew that key (never order-service's own generated id)
    // can still look its order up, which is exactly what a frontend needs
    // to learn the order id after the booking-service leg of the saga.
    fun findByIdempotencyKey(idempotencyKey: String): OrderView?
}

// Lives next to the interface, not in api, for the same reason
// BookingEntity.toView() lives in booking-service's internal/service --
// nothing outside this module ever constructs an OrderView from a raw
// OrderEntity, so that mapping is an implementation detail, not part of the
// api surface.
fun OrderEntity.toView(justCreated: Boolean = false) = OrderView(
    orderId = id,
    status = status,
    offerId = offerId,
    duffelOrderId = duffelOrderId,
    bookingReference = bookingReference,
    failureReason = failureReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
    justCreated = justCreated,
)
