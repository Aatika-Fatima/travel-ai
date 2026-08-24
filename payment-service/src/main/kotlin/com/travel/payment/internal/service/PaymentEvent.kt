package com.travel.payment.internal.service

import com.travel.payment.internal.entity.Payment
import com.travel.payment.internal.entity.PaymentStatus

sealed interface PaymentEvent{
    data class Attempted(val razorpayPaymentId:String):PaymentEvent
    data class Captured(val razorpayPaymentId:String):PaymentEvent
    data class Failed(val reason: String) : PaymentEvent
    data class Refunded(val reason: String) : PaymentEvent
}

// CAPTURED -> ATTEMPTED still throws, loudly, rather than walking captured
// money back to a non-terminal state. Two edges look like exceptions to
// that rule but aren't -- they're Razorpay telling us something happened
// on its side, not this service reversing itself:
//   FAILED -> CAPTURED: Razorpay documents payment.failed followed by
//   payment.captured for the SAME payment_id as expected behaviour -- late
//   authorisation, or a customer retrying a failed UPI collect request.
//   Treating FAILED as terminal here would leave a payment Razorpay
//   actually captured stuck FAILED forever, since P9's reconciliation
//   sweep didn't even look at FAILED rows.
//   CAPTURED -> REFUNDED: a full refund flips the payment's own status on
//   Razorpay's side, independent of anything this service does. REFUNDED
//   isn't a failure mode -- it's money that moved, then moved back.
fun Payment.applyEvent(event: PaymentEvent): Boolean = when (event) {
    is PaymentEvent.Attempted -> when (status) {
        PaymentStatus.CREATED -> { razorpayPaymentId = event.razorpayPaymentId; status = PaymentStatus.ATTEMPTED; true }
        PaymentStatus.ATTEMPTED -> false // already there -- idempotent no-op
        else -> throw IllegalStateException("Cannot apply $event to payment $id in state $status")
    }
    is PaymentEvent.Captured -> when (status) {
        PaymentStatus.CREATED, PaymentStatus.ATTEMPTED, PaymentStatus.FAILED ->
        { razorpayPaymentId = event.razorpayPaymentId; failureReason = null; status = PaymentStatus.CAPTURED; true } // FAILED here is the late-capture recovery edge
        PaymentStatus.CAPTURED -> false // second signal for the same success -- no-op, not an error
        else -> throw IllegalStateException("Cannot apply $event to payment $id in state $status")
    }
    is PaymentEvent.Failed -> when (status) {
        PaymentStatus.CREATED, PaymentStatus.ATTEMPTED -> { failureReason = event.reason; status = PaymentStatus.FAILED; true }
        PaymentStatus.FAILED -> false
        // CAPTURED -> FAILED is never legal from here -- refunds are a separate, explicit event
        else -> throw IllegalStateException("Cannot apply $event to payment $id in state $status")
    }
    is PaymentEvent.Refunded -> when (status) {
        PaymentStatus.CAPTURED ->
        { failureReason = event.reason; status = PaymentStatus.REFUNDED; true } // reuses failureReason as a general terminal-state note
        PaymentStatus.REFUNDED -> false // second signal for the same refund -- no-op
        // can't refund money this service never recorded as captured
        else -> throw IllegalStateException("Cannot apply $event to payment $id in state $status")
    }
}