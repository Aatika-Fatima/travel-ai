package com.travel.payment.internal.controller

import com.travel.payment.internal.controller.dto.ConfirmRequest
import com.travel.payment.internal.controller.dto.CreateOrderRequest
import com.travel.payment.internal.controller.dto.PaymentOrderResponse
import com.travel.payment.internal.gateway.razorpay.RazorpayGateway
import com.travel.payment.internal.gateway.razorpay.RazorpayProperties
import com.travel.payment.internal.repository.PaymentTransactionalOps
import com.travel.payment.internal.service.PaymentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import kotlin.uuid.Uuid

@RestController
class PaymentController(
    val gateway: RazorpayGateway,
    val transactionalOps: PaymentTransactionalOps,
    val paymentService: PaymentService,
    val razorpayProperties: RazorpayProperties,
) {

    // No Idempotency-Key header -- PaymentService.createOrder derives its own
    // dedup key from bookingId (one payment order per booking), so a header
    // here would just be a second, unused source of truth for the same thing.
    @PostMapping("/payments")
    fun create(@RequestBody @Valid request: CreateOrderRequest): ResponseEntity<PaymentOrderResponse> {
        val payment = paymentService.createOrder(
            idempotencyKey = "booking:${request.bookingId}",
            bookingId = request.bookingId,
            amountPaise = request.amountPaise,
            currency = request.currency,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            PaymentOrderResponse(
                paymentId = payment.id,
                razorpayOrderId = payment.razorpayOrderId,
                amountPaise = payment.amountPaise,
                currency = payment.currency,
                status = payment.status.name,
                keyId = razorpayProperties.apiKey,
            ),
        )
    }

    @PostMapping("/payments/{bookingId}/confirm")
    fun confirm(@PathVariable bookingId: Uuid, @RequestBody confirmRequest: ConfirmRequest): ResponseEntity<Unit> {

        if(!gateway.verifyCheckoutSignature(confirmRequest.razorpayOrderId, confirmRequest.razorpayPaymentId, confirmRequest.razorpaySignature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        transactionalOps.confirmCaptured(bookingId, confirmRequest.razorpayPaymentId) // P5's transition, P8's tx shape
        // 204, not 200 -- the frontend's shared request() helper only treats
        // an empty body as valid on 204; a 200 with no body throws trying to
        // JSON-parse it.
        return ResponseEntity.noContent().build()
    }
}