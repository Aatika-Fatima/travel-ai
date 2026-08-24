package com.travel.payment.internal.service

import com.travel.payment.internal.entity.Payment
import com.travel.payment.internal.entity.PaymentStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class PaymentEventTest {
    private fun payment(status: PaymentStatus = PaymentStatus.CREATED, failureReason: String? = null) =
        Payment(
            bookingId = Uuid.random(),
            idempotencyKey = "booking:${Uuid.random()}",
            amountPaise = 10_000,
            currency = "INR",
            status = status,
            failureReason = failureReason,
        )

    @Test
    fun `Attempted from CREATED transitions to ATTEMPTED and records the payment id`() {
        val payment = payment(PaymentStatus.CREATED)

        val changed = payment.applyEvent(PaymentEvent.Attempted("pay_1"))

        assertTrue(changed)
        assertEquals(PaymentStatus.ATTEMPTED, payment.status)
        assertEquals("pay_1", payment.razorpayPaymentId)
    }

    @Test
    fun `Attempted from ATTEMPTED is a no-op`() {
        val payment = payment(PaymentStatus.ATTEMPTED)

        val changed = payment.applyEvent(PaymentEvent.Attempted("pay_1"))

        assertFalse(changed)
        assertEquals(PaymentStatus.ATTEMPTED, payment.status)
    }

    @Test
    fun `Attempted from CAPTURED is illegal`() {
        val payment = payment(PaymentStatus.CAPTURED)

        assertFailsWith<IllegalStateException> { payment.applyEvent(PaymentEvent.Attempted("pay_1")) }
    }

    @Test
    fun `Captured from CREATED transitions to CAPTURED`() {
        val payment = payment(PaymentStatus.CREATED)

        val changed = payment.applyEvent(PaymentEvent.Captured("pay_1"))

        assertTrue(changed)
        assertEquals(PaymentStatus.CAPTURED, payment.status)
        assertEquals("pay_1", payment.razorpayPaymentId)
    }

    @Test
    fun `Captured from ATTEMPTED transitions to CAPTURED`() {
        val payment = payment(PaymentStatus.ATTEMPTED)

        val changed = payment.applyEvent(PaymentEvent.Captured("pay_1"))

        assertTrue(changed)
        assertEquals(PaymentStatus.CAPTURED, payment.status)
    }

    @Test
    fun `Captured from FAILED is the late-capture recovery edge and clears the failure reason`() {
        val payment = payment(PaymentStatus.FAILED, failureReason = "insufficient_funds")

        val changed = payment.applyEvent(PaymentEvent.Captured("pay_1"))

        assertTrue(changed)
        assertEquals(PaymentStatus.CAPTURED, payment.status)
        assertNull(payment.failureReason)
    }

    @Test
    fun `Captured from CAPTURED is a no-op`() {
        val payment = payment(PaymentStatus.CAPTURED)

        val changed = payment.applyEvent(PaymentEvent.Captured("pay_1"))

        assertFalse(changed)
    }

    @Test
    fun `Captured from REFUNDED is illegal`() {
        val payment = payment(PaymentStatus.REFUNDED)

        assertFailsWith<IllegalStateException> { payment.applyEvent(PaymentEvent.Captured("pay_1")) }
    }

    @Test
    fun `Failed from CREATED transitions to FAILED and records the reason`() {
        val payment = payment(PaymentStatus.CREATED)

        val changed = payment.applyEvent(PaymentEvent.Failed("card_declined"))

        assertTrue(changed)
        assertEquals(PaymentStatus.FAILED, payment.status)
        assertEquals("card_declined", payment.failureReason)
    }

    @Test
    fun `Failed from ATTEMPTED transitions to FAILED`() {
        val payment = payment(PaymentStatus.ATTEMPTED)

        val changed = payment.applyEvent(PaymentEvent.Failed("card_declined"))

        assertTrue(changed)
        assertEquals(PaymentStatus.FAILED, payment.status)
    }

    @Test
    fun `Failed from FAILED is a no-op`() {
        val payment = payment(PaymentStatus.FAILED, failureReason = "card_declined")

        val changed = payment.applyEvent(PaymentEvent.Failed("card_declined"))

        assertFalse(changed)
    }

    @Test
    fun `Failed from CAPTURED is illegal -- refunds are a separate event`() {
        val payment = payment(PaymentStatus.CAPTURED)

        assertFailsWith<IllegalStateException> { payment.applyEvent(PaymentEvent.Failed("card_declined")) }
    }

    @Test
    fun `Refunded from CAPTURED transitions to REFUNDED and records the reason`() {
        val payment = payment(PaymentStatus.CAPTURED)

        val changed = payment.applyEvent(PaymentEvent.Refunded("customer requested"))

        assertTrue(changed)
        assertEquals(PaymentStatus.REFUNDED, payment.status)
        assertEquals("customer requested", payment.failureReason)
    }

    @Test
    fun `Refunded from REFUNDED is a no-op`() {
        val payment = payment(PaymentStatus.REFUNDED, failureReason = "customer requested")

        val changed = payment.applyEvent(PaymentEvent.Refunded("customer requested"))

        assertFalse(changed)
    }

    @Test
    fun `Refunded from CREATED is illegal -- can't refund money never recorded as captured`() {
        val payment = payment(PaymentStatus.CREATED)

        assertFailsWith<IllegalStateException> { payment.applyEvent(PaymentEvent.Refunded("customer requested")) }
    }
}
