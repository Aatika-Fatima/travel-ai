package com.travel.payment.internal.controller

import com.travel.payment.internal.controller.dto.ConfirmRequest
import com.travel.payment.internal.controller.dto.CreateOrderRequest
import com.travel.payment.internal.entity.Payment
import com.travel.payment.internal.entity.PaymentStatus
import com.travel.payment.internal.gateway.razorpay.RazorpayGateway
import com.travel.payment.internal.gateway.razorpay.RazorpayProperties
import com.travel.payment.internal.repository.PaymentTransactionalOps
import com.travel.payment.internal.service.PaymentService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class PaymentControllerTest {
    private val gateway: RazorpayGateway = mock()
    private val transactionalOps: PaymentTransactionalOps = mock()
    private val paymentService: PaymentService = mock()
    private val razorpayProperties = RazorpayProperties(apiKey = "rzp_test_key", apiSecret = "secret", webhookSecret = "whsecret")
    private val controller = PaymentController(gateway, transactionalOps, paymentService, razorpayProperties)

    @Test
    fun `create derives the idempotency key from bookingId and returns the created payment`() {
        val bookingId = Uuid.random()
        val request = CreateOrderRequest(bookingId = bookingId, amountPaise = 12345, currency = "INR")
        val payment = Payment(
            bookingId = bookingId,
            idempotencyKey = "booking:$bookingId",
            amountPaise = 12345,
            currency = "INR",
            razorpayOrderId = "order_1",
            status = PaymentStatus.ATTEMPTED,
        )
        whenever(paymentService.createOrder(eq("booking:$bookingId"), eq(bookingId), eq(12345L), eq("INR")))
            .thenReturn(payment)

        val response = controller.create(request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body!!
        assertEquals(payment.id, body.paymentId)
        assertEquals("order_1", body.razorpayOrderId)
        assertEquals(12345L, body.amountPaise)
        assertEquals("INR", body.currency)
        assertEquals("ATTEMPTED", body.status)
        assertEquals("rzp_test_key", body.keyId)
    }

    @Test
    fun `confirm returns 401 and never confirms capture when the signature is invalid`() {
        val bookingId = Uuid.random()
        val req = ConfirmRequest("order_1", "pay_1", "bad-sig")
        whenever(gateway.verifyCheckoutSignature("order_1", "pay_1", "bad-sig")).thenReturn(false)

        val response = controller.confirm(bookingId, req)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        verify(transactionalOps, never()).confirmCaptured(any(), any())
    }

    @Test
    fun `confirm captures the payment and returns 204 when the signature is valid`() {
        val bookingId = Uuid.random()
        val req = ConfirmRequest("order_1", "pay_1", "good-sig")
        whenever(gateway.verifyCheckoutSignature("order_1", "pay_1", "good-sig")).thenReturn(true)

        val response = controller.confirm(bookingId, req)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        verify(transactionalOps).confirmCaptured(bookingId, "pay_1")
    }
}
