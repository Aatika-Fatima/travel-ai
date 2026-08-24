package com.travel.orderservice.api

import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

// The REST body -- idempotencyKey deliberately isn't a field here. It's a
// client-generated header (see OrderController), not part of the booking
// details themselves -- the same split duffle-api/flight already makes
// between BookingRequest and idempotencyKey (P3).
data class SubmitOrderRequest(
    @field:NotBlank
    val offerId: String,

    @field:NotEmpty
    @field:Valid
    val passengers: List<PassengerDetails>,

    @field:Valid
    val contact: ContactInfo,

    @field:NotBlank
    val paymentType: String = "balance",
)

fun SubmitOrderRequest.toCommand(idempotencyKey: String) = SubmitOrderCommand(
    idempotencyKey = idempotencyKey,
    offerId = offerId,
    passengers = passengers,
    contact = contact,
    paymentType = paymentType,
)
