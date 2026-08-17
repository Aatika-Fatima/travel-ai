package com.travel.payment.internal.gateway.razorpay

interface RazorpayGateway {
    // idempotencyKey is always the caller's already-persisted key, sent as
    // Razorpay's `receipt` -- this method never invents one itself.
    fun createOrder(amountPaise: Long, currency: String, idempotencyKey: String): RazorpayOrderResult

    fun capture(razorpayPaymentId: String, amountPaise: Long, currency: String): RazorpayCaptureResult

    fun verifyCheckoutSignature(orderId: String, paymentId: String, signature: String): Boolean

    fun verifyWebhookSignature(rawBody: String, signature: String): Boolean
}

data class RazorpayOrderResult(val razorpayOrderId: String, val status: String)
data class RazorpayCaptureResult(val razorpayPaymentId: String, val status: String)

// Thrown only for the one case P4 must treat specially -- see impl below.
class ReceiptAlreadyExistsException(val receipt: String) : RuntimeException(
    "Razorpay rejected receipt=$receipt as a duplicate"
)