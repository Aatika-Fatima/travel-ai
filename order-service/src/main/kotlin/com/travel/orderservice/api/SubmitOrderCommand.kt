package com.travel.orderservice.api

import com.travel.common.model.BookingRequest
import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import kotlin.uuid.Uuid

// order-service's own submission shape -- built once, either from a REST
// body (SubmitOrderRequest.toCommand()) or from a Kafka BookingCreatedPayload
// (P11) -- and from that point on the rest of this service never looks at
// either origin type again.
data class SubmitOrderCommand(
    val idempotencyKey: String,
    val offerId: String,
    val passengers: List<PassengerDetails>,
    val contact: ContactInfo,
    val paymentType: String = "balance",
    // Set only when this command originated from booking-service's own
    // BookingCreated event -- order-service's order id is then seeded from
    // booking-service's booking id (see BookingCreatedPayload.toSubmitOrderCommand
    // and booking-service's own OrderEventConsumer step-note), so the SAME
    // id correlates an order back to its originating booking with no extra
    // column needed. Null for a direct REST submission, which has no
    // booking-service booking to correlate with.
    val orderId: Uuid? = null,
)

// Rebuilds exactly what duffle-api/flight's DuffelBookingService (P3)
// expects -- the one point where order-service's own command shape gets
// translated into the gateway's.
fun SubmitOrderCommand.toBookingRequest() = BookingRequest(
    offerId = offerId,
    passengers = passengers,
    contact = contact,
    paymentType = paymentType,
)
