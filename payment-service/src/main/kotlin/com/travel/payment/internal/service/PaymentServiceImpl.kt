package com.travel.payment.internal.service

import com.travel.payment.internal.entity.Payment
import com.travel.payment.internal.gateway.razorpay.RazorpayGateway
import com.travel.payment.internal.gateway.razorpay.ReceiptAlreadyExistsException
import com.travel.payment.internal.repository.PaymentRepository
import com.travel.payment.internal.repository.PaymentTransactionalOps
import kotlin.uuid.Uuid

class PaymentServiceImpl(
    private val repository: PaymentRepository,
    private val transactionalOps: PaymentTransactionalOps, // see P8 -- separate bean on purpose
    private val gateway: RazorpayGateway,
): PaymentService {
    override fun createOrder(
        idempotencyKey: String,
        bookingId: Uuid,
        amountPaise: Long,
        currency: String
    ): Payment {

        val idempotencyKey = "booking:$bookingId"
        transactionalOps.findOrInsertPending(bookingId,idempotencyKey,amountPaise,currency)
        val (payment,alreadyExisted) = transactionalOps.findOrInsertPending(bookingId,idempotencyKey,amountPaise,currency)
        if(alreadyExisted) return payment
        val result = try{
            gateway.createOrder(amountPaise, currency, idempotencyKey)
        }catch (e: ReceiptAlreadyExistsException){
            // Razorpay says this receipt was already used, but -- unlike a
            // Stripe-style idempotent replay -- it doesn't hand back the
            // order id. This should be unreachable in the normal case,
            // since P1's unique constraint already stopped a second
            // INSERT for this key; if it happens anyway (e.g. a crash
            // between insert and gateway call on a previous attempt,
            // then a manual retry with a stale key), fail loudly rather
            // than guess -- P9's reconciliation job is what resolves it.
            throw PaymentOrderAmbiguousException(bookingId, idempotencyKey, e)
        }
        // Step 3 -- short transaction again, recording the Razorpay order id.
        return transactionalOps.recordRazorPayOrder(payment.id, result.razorpayOrderId.toString())
     }
}

class PaymentOrderAmbiguousException(
    bookingId: Uuid,
    idempotencyKey: String,
    e: ReceiptAlreadyExistsException
): Exception("$bookingId: $idempotencyKey exists at razor pay",  e)