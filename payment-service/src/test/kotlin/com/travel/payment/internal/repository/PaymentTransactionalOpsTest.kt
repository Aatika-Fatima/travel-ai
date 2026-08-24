package com.travel.payment.internal.repository

import com.travel.payment.internal.entity.Payment
import com.travel.payment.internal.entity.PaymentStatus
import com.travel.payment.internal.entity.WebhookEvent
import com.travel.payment.internal.outbox.PaymentOutboxWriter
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class PaymentTransactionalOpsTest {
    private val paymentRepository: PaymentRepository = mock()
    private val webhookEventRepository: WebhookEventRepository = mock()
    private val outboxWriter: PaymentOutboxWriter = mock()
    private val ops = PaymentTransactionalOps(paymentRepository, webhookEventRepository, outboxWriter)

    private fun payment(status: PaymentStatus = PaymentStatus.CREATED, bookingId: Uuid = Uuid.random()) =
        Payment(
            bookingId = bookingId,
            idempotencyKey = "booking:$bookingId",
            amountPaise = 1_000,
            currency = "INR",
            status = status,
        )

    private fun paymentPayload(
        event: String,
        orderId: String = "order_1",
        paymentId: String = "pay_1",
        errorDescription: String? = null,
        errorReason: String? = null,
    ): JSONObject {
        val entity = JSONObject().put("id", paymentId).put("order_id", orderId)
        errorDescription?.let { entity.put("error_description", it) }
        errorReason?.let { entity.put("error_reason", it) }
        val payload = JSONObject().put("payment", JSONObject().put("entity", entity))
        return JSONObject().put("event", event).put("payload", payload)
    }

    private fun refundPayload(
        event: String,
        orderId: String = "order_1",
        paymentId: String = "pay_1",
        refundId: String = "rfnd_1",
        status: String = "processed",
        notes: String? = null,
    ): JSONObject {
        val paymentEntity = JSONObject().put("id", paymentId).put("order_id", orderId)
        val refundEntity = JSONObject().put("id", refundId).put("status", status)
        notes?.let { refundEntity.put("notes", it) }
        val payload =
            JSONObject()
                .put("payment", JSONObject().put("entity", paymentEntity))
                .put("refund", JSONObject().put("entity", refundEntity))
        return JSONObject().put("event", event).put("payload", payload)
    }

    // ---- findOrInsertPending ----

    @Test
    fun `findOrInsertPending returns the existing row when the idempotency key already matches`() {
        val existing = payment()
        whenever(paymentRepository.findByIdempotencyKey("key")).thenReturn(existing)

        val (result, alreadyExisted) = ops.findOrInsertPending(existing.bookingId, "key", 1_000, "INR")

        assertEquals(existing, result)
        assertTrue(alreadyExisted)
        verify(paymentRepository, never()).saveAndFlush(any<Payment>())
    }

    @Test
    fun `findOrInsertPending inserts a new row when no key match exists`() {
        whenever(paymentRepository.findByIdempotencyKey("key")).thenReturn(null)
        val saved = payment()
        whenever(paymentRepository.saveAndFlush(any<Payment>())).thenReturn(saved)

        val (result, alreadyExisted) = ops.findOrInsertPending(saved.bookingId, "key", 1_000, "INR")

        assertEquals(saved, result)
        assertFalse(alreadyExisted)
    }

    @Test
    fun `findOrInsertPending reads back the winner on a lost insert race`() {
        val winner = payment()
        whenever(paymentRepository.findByIdempotencyKey("key")).thenReturn(null, winner)
        whenever(paymentRepository.saveAndFlush(any<Payment>())).thenThrow(DataIntegrityViolationException("dup"))

        val (result, alreadyExisted) = ops.findOrInsertPending(winner.bookingId, "key", 1_000, "INR")

        assertEquals(winner, result)
        assertTrue(alreadyExisted)
    }

    @Test
    fun `findOrInsertPending rethrows the original exception when the winner can't be found on re-read`() {
        val ex = DataIntegrityViolationException("dup")
        whenever(paymentRepository.findByIdempotencyKey("key")).thenReturn(null)
        whenever(paymentRepository.saveAndFlush(any<Payment>())).thenThrow(ex)

        val thrown =
            assertFailsWith<DataIntegrityViolationException> {
                ops.findOrInsertPending(Uuid.random(), "key", 1_000, "INR")
            }
        assertEquals(ex, thrown)
    }

    // ---- recordRazorPayOrder ----

    @Test
    fun `recordRazorPayOrder sets the order id and marks the payment ATTEMPTED`() {
        val existing = payment(PaymentStatus.CREATED)
        whenever(paymentRepository.findById(existing.id)).thenReturn(Optional.of(existing))

        val result = ops.recordRazorPayOrder(existing.id, "order_1")

        assertEquals("order_1", result.razorpayOrderId)
        assertEquals(PaymentStatus.ATTEMPTED, result.status)
    }

    @Test
    fun `recordRazorPayOrder throws when no payment row exists for the id`() {
        val paymentId = Uuid.random()
        whenever(paymentRepository.findById(paymentId)).thenReturn(Optional.empty())

        assertFailsWith<NullPointerException> { ops.recordRazorPayOrder(paymentId, "order_1") }
    }

    // ---- confirmCaptured ----

    @Test
    fun `confirmCaptured throws when no payment exists for the booking`() {
        val bookingId = Uuid.random()
        whenever(paymentRepository.findByBookingId(bookingId)).thenReturn(null)

        assertFailsWith<PaymentNotFoundException> { ops.confirmCaptured(bookingId, "pay_1") }
    }

    @Test
    fun `confirmCaptured transitions to CAPTURED, saves, and publishes when it wins the race`() {
        val existing = payment(PaymentStatus.ATTEMPTED)
        whenever(paymentRepository.findByBookingId(existing.bookingId)).thenReturn(existing)

        ops.confirmCaptured(existing.bookingId, "pay_1")

        assertEquals(PaymentStatus.CAPTURED, existing.status)
        verify(paymentRepository).save(existing)
        verify(outboxWriter).enqueue(eq("payment.captured"), eq(existing.bookingId), any())
    }

    @Test
    fun `confirmCaptured saves but does not publish again when the webhook already won`() {
        val existing = payment(PaymentStatus.CAPTURED)
        whenever(paymentRepository.findByBookingId(existing.bookingId)).thenReturn(existing)

        ops.confirmCaptured(existing.bookingId, "pay_1")

        verify(paymentRepository).save(existing)
        verify(outboxWriter, never()).enqueue(any(), any(), any())
    }

    // ---- processWebhookOnce ----

    @Test
    fun `processWebhookOnce returns DUPLICATE without touching the payment when the dedup insert collides`() {
        whenever(webhookEventRepository.saveAndFlush(any<WebhookEvent>()))
            .thenThrow(DataIntegrityViolationException("dup"))

        val outcome = ops.processWebhookOnce("payment.captured", "pay_1", paymentPayload("payment.captured"))

        assertEquals(WebhookOutcome.DUPLICATE, outcome)
        verify(paymentRepository, never()).findByRazorpayOrderId(any())
    }

    @Test
    fun `processWebhookOnce ignores event types this service doesn't act on`() {
        val outcome =
            ops.processWebhookOnce(
                "payment.downtime.started",
                "payment.downtime.started:0",
                JSONObject().put("event", "payment.downtime.started"),
            )

        assertEquals(WebhookOutcome.IGNORED_EVENT, outcome)
    }

    @Test
    fun `processWebhookOnce acks an unknown order without acting`() {
        whenever(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(null)

        val outcome = ops.processWebhookOnce("payment.captured", "pay_1", paymentPayload("payment.captured"))

        assertEquals(WebhookOutcome.IGNORED_EVENT, outcome)
    }

    @Test
    fun `processWebhookOnce acks an unknown payment on a refund event without acting`() {
        whenever(paymentRepository.findByRazorpayPaymentId("pay_1")).thenReturn(null)

        val outcome = ops.processWebhookOnce("refund.processed", "rfnd_1", refundPayload("refund.processed"))

        assertEquals(WebhookOutcome.IGNORED_EVENT, outcome)
    }

    @Test
    fun `processWebhookOnce captures on payment_captured and publishes once`() {
        val existing = payment(PaymentStatus.ATTEMPTED)
        whenever(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(existing)
        val payload = paymentPayload("payment.captured")

        val outcome = ops.processWebhookOnce("payment.captured", "pay_1", payload)

        assertEquals(WebhookOutcome.PROCESSED, outcome)
        assertEquals(PaymentStatus.CAPTURED, existing.status)
        verify(outboxWriter).enqueue(eq("payment.captured"), eq(existing.bookingId), any())

        val webhookEventCaptor = argumentCaptor<WebhookEvent>()
        verify(webhookEventRepository).saveAndFlush(webhookEventCaptor.capture())
        val storedEvent = webhookEventCaptor.firstValue
        assertEquals("payment.captured", storedEvent.event)
        assertEquals("pay_1", storedEvent.entityId)
        assertEquals(payload.toString(), storedEvent.payload)
    }

    @Test
    fun `processWebhookOnce captures on order_paid too`() {
        val existing = payment(PaymentStatus.ATTEMPTED)
        whenever(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(existing)

        val outcome = ops.processWebhookOnce("order.paid", "pay_1", paymentPayload("order.paid"))

        assertEquals(WebhookOutcome.PROCESSED, outcome)
        assertEquals(PaymentStatus.CAPTURED, existing.status)
    }

    @Test
    fun `processWebhookOnce is a silent no-op when the webhook redelivers a capture already applied`() {
        val existing = payment(PaymentStatus.CAPTURED)
        whenever(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(existing)

        val outcome = ops.processWebhookOnce("payment.captured", "pay_1", paymentPayload("payment.captured"))

        assertEquals(WebhookOutcome.PROCESSED, outcome)
        verify(outboxWriter, never()).enqueue(any(), any(), any())
    }

    @Test
    fun `processWebhookOnce marks FAILED using the error description from the payload`() {
        val existing = payment(PaymentStatus.ATTEMPTED)
        whenever(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(existing)

        val outcome =
            ops.processWebhookOnce(
                "payment.failed",
                "pay_1",
                paymentPayload("payment.failed", errorDescription = "Card declined"),
            )

        assertEquals(WebhookOutcome.PROCESSED, outcome)
        assertEquals(PaymentStatus.FAILED, existing.status)
        assertEquals("Card declined", existing.failureReason)
        verify(outboxWriter, never()).enqueue(any(), any(), any())
    }

    @Test
    fun `processWebhookOnce falls back to error_reason when error_description is blank`() {
        val existing = payment(PaymentStatus.ATTEMPTED)
        whenever(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(existing)

        ops.processWebhookOnce(
            "payment.failed",
            "pay_1",
            paymentPayload("payment.failed", errorDescription = "", errorReason = "insufficient_funds"),
        )

        assertEquals("insufficient_funds", existing.failureReason)
    }

    @Test
    fun `processWebhookOnce falls back to a generic reason when no error fields are present`() {
        val existing = payment(PaymentStatus.ATTEMPTED)
        whenever(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(existing)

        ops.processWebhookOnce("payment.failed", "pay_1", paymentPayload("payment.failed"))

        assertEquals("payment failed", existing.failureReason)
    }

    @Test
    fun `processWebhookOnce refunds a captured payment and publishes payment_refunded`() {
        val existing = payment(PaymentStatus.CAPTURED)
        whenever(paymentRepository.findByRazorpayPaymentId("pay_1")).thenReturn(existing)

        val outcome =
            ops.processWebhookOnce(
                "refund.processed",
                "rfnd_1",
                refundPayload("refund.processed", notes = "customer requested"),
            )

        assertEquals(WebhookOutcome.PROCESSED, outcome)
        assertEquals(PaymentStatus.REFUNDED, existing.status)
        assertEquals("customer requested", existing.failureReason)
        verify(outboxWriter).enqueue(eq("payment.refunded"), eq(existing.bookingId), any())
    }

    @Test
    fun `processWebhookOnce falls back to a status-based reason when refund notes are blank`() {
        val existing = payment(PaymentStatus.CAPTURED)
        whenever(paymentRepository.findByRazorpayPaymentId("pay_1")).thenReturn(existing)

        ops.processWebhookOnce("refund.processed", "rfnd_1", refundPayload("refund.processed", status = "processed"))

        assertEquals("refund processed", existing.failureReason)
    }

    @Test
    fun `processWebhookOnce does not transition on refund_failed -- the payment stays CAPTURED`() {
        val existing = payment(PaymentStatus.CAPTURED)
        whenever(paymentRepository.findByRazorpayPaymentId("pay_1")).thenReturn(existing)

        val outcome = ops.processWebhookOnce("refund.failed", "rfnd_1", refundPayload("refund.failed"))

        assertEquals(WebhookOutcome.PROCESSED, outcome)
        assertEquals(PaymentStatus.CAPTURED, existing.status)
        verify(outboxWriter, never()).enqueue(any(), any(), any())
    }
}
