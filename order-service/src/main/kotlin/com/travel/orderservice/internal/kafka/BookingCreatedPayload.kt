package com.travel.orderservice.internal.kafka

import com.travel.common.model.BookingRequest
import com.travel.orderservice.api.SubmitOrderCommand
import kotlin.uuid.Uuid

// A local mirror of booking-service's own internal.service.BookingCreatedPayload
// (booking_service.html §P3) -- order-service can't import that class
// directly, since it lives in booking-service's internal package, off
// limits across the module boundary per ModuleBoundaryArchTest. Same JSON
// shape, redeclared on this side of the wire.
data class BookingCreatedPayload(
    val bookingId: String,
    val idempotencyKey: String,
    val offerId: String,
    val request: BookingRequest,
)

fun BookingCreatedPayload.toSubmitOrderCommand() = SubmitOrderCommand(
    idempotencyKey = idempotencyKey,
    offerId = offerId,
    passengers = request.passengers,
    contact = request.contact,
    paymentType = request.paymentType,
    // Seeds order-service's own order id from booking-service's booking id
    // -- see SubmitOrderCommand.orderId's own doc comment for why.
    orderId = Uuid.parse(bookingId),
)
