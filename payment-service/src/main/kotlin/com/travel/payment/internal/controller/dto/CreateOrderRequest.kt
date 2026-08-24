package com.travel.payment.internal.controller.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import kotlin.uuid.Uuid

data class CreateOrderRequest(
    val bookingId: Uuid,
    @field:Positive
    val amountPaise: Long,
    @field:NotBlank
    val currency: String,
)

data class PaymentOrderResponse(
    val paymentId: Uuid,
    val razorpayOrderId: String?,
    val amountPaise: Long,
    val currency: String,
    val status: String,
    // Razorpay's key_id -- safe to expose to the browser (it's the
    // publishable half of the credential pair, not key_secret); Checkout.js
    // needs it alongside razorpayOrderId to open the payment sheet.
    val keyId: String,
)
