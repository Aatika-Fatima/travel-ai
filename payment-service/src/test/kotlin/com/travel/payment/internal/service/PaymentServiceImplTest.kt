package com.travel.payment.internal.service

import com.travel.payment.internal.entity.Payment
import com.travel.payment.internal.entity.PaymentStatus
import com.travel.payment.internal.gateway.razorpay.RazorpayGateway
import com.travel.payment.internal.gateway.razorpay.RazorpayOrderResult
import com.travel.payment.internal.gateway.razorpay.ReceiptAlreadyExistsException
import com.travel.payment.internal.repository.PaymentRepository
import com.travel.payment.internal.repository.PaymentTransactionalOps
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid

class PaymentServiceImplTest {
    private val repository: PaymentRepository = mock()
    private val transactionalOps: PaymentTransactionalOps = mock()
    private val gateway: RazorpayGateway = mock()
    private val service = PaymentServiceImpl(repository, transactionalOps, gateway)

    private fun payment(bookingId: Uuid, status: PaymentStatus = PaymentStatus.CREATED) =
        Payment(
            bookingId = bookingId,
            idempotencyKey = "booking:$bookingId",
            amountPaise = 1_000,
            currency = "INR",
            status = status,
        )

    @Test
    fun `createOrder returns the existing pending row without calling Razorpay when it already exists`() {
        val bookingId = Uuid.random()
        val existing = payment(bookingId)
        whenever(transactionalOps.findOrInsertPending(eq(bookingId), any(), any(), any()))
            .thenReturn(existing to true)

        val result = service.createOrder("ignored", bookingId, 1_000, "INR")

        assertEquals(existing, result)
        verify(gateway, never()).createOrder(any(), any(), any())
        verify(transactionalOps, never()).recordRazorPayOrder(any(), any())
    }

    @Test
    fun `createOrder calls Razorpay and records the order id for a fresh pending row`() {
        val bookingId = Uuid.random()
        val pending = payment(bookingId)
        whenever(transactionalOps.findOrInsertPending(eq(bookingId), any(), any(), any()))
            .thenReturn(pending to false)
        whenever(gateway.createOrder(eq(1_000L), eq("INR"), eq("booking:$bookingId")))
            .thenReturn(RazorpayOrderResult("order_123", "created"))
        val recorded = payment(bookingId, PaymentStatus.ATTEMPTED).also { it.razorpayOrderId = "order_123" }
        whenever(transactionalOps.recordRazorPayOrder(pending.id, "order_123")).thenReturn(recorded)

        val result = service.createOrder("ignored", bookingId, 1_000, "INR")

        assertEquals(recorded, result)
        verify(transactionalOps).recordRazorPayOrder(pending.id, "order_123")
    }

    @Test
    fun `createOrder wraps a duplicate-receipt rejection from Razorpay as PaymentOrderAmbiguousException`() {
        val bookingId = Uuid.random()
        val pending = payment(bookingId)
        whenever(transactionalOps.findOrInsertPending(eq(bookingId), any(), any(), any()))
            .thenReturn(pending to false)
        whenever(gateway.createOrder(any(), any(), any()))
            .thenThrow(ReceiptAlreadyExistsException("booking:$bookingId"))

        assertFailsWith<PaymentOrderAmbiguousException> {
            service.createOrder("ignored", bookingId, 1_000, "INR")
        }
        verify(transactionalOps, never()).recordRazorPayOrder(any(), any())
    }
}
