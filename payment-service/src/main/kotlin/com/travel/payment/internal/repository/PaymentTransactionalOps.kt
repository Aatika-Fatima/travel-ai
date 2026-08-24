package com.travel.payment.internal.repository

 import com.travel.payment.internal.entity.Payment
 import com.travel.payment.internal.entity.PaymentStatus
 import com.travel.payment.internal.entity.WebhookEvent
 import com.travel.payment.internal.outbox.PaymentOutboxWriter
 import com.travel.payment.internal.service.PaymentEvent
 import com.travel.payment.internal.service.applyEvent
 import org.json.JSONObject
 import org.springframework.dao.DataIntegrityViolationException
 import org.springframework.stereotype.Component
 import org.springframework.transaction.annotation.Transactional
 import java.util.UUID
 import kotlin.jvm.optionals.getOrNull
 import kotlin.uuid.Uuid

@Component
class PaymentTransactionalOps(
    private val paymentRepository: PaymentRepository,
    private val webhookEventRepository: WebhookEventRepository,
    private val outboxWriter: PaymentOutboxWriter,
) {

    @Transactional
    fun findOrInsertPending(bookingId: Uuid,idempotencyKey:String, amountInPaise:Long, currency: String): Pair<Payment, Boolean> {
        paymentRepository.findByIdempotencyKey(idempotencyKey)?.let { return it to true }
        return try{
            val saved= paymentRepository.saveAndFlush(
                Payment(bookingId=bookingId, idempotencyKey = idempotencyKey, amountPaise = amountInPaise,currency = currency)
            )
            saved to false
        }catch (ex: DataIntegrityViolationException){
            val entity = paymentRepository .findByIdempotencyKey(idempotencyKey) ?: throw ex
            entity to true
        }
    }

    @Transactional
    fun recordRazorPayOrder(paymentId:Uuid,razorPayOrderId: String): Payment {
        val payment = paymentRepository.findById(paymentId).getOrNull()
        payment?.razorpayOrderId = razorPayOrderId
        payment?.status = PaymentStatus.ATTEMPTED
        return payment as Payment

    }

    @Transactional
    fun confirmCaptured(bookingId: Uuid, razorpayPaymentId: String) {
        val payment = paymentRepository.findByBookingId(bookingId)
            ?: throw PaymentNotFoundException(bookingId)
        // Same transition P7's webhook handler drives. Whichever signal --
        // this checkout confirm, or the webhook -- reaches CAPTURED first
        // wins; the other's applyEvent call is a safe, silent no-op, not
        // an error, since both are just proof of the same underlying event.
        // Only the caller that actually made the transition publishes --
        // that's what stops the booking-confirmation email going out twice.
        val changed = payment.applyEvent(PaymentEvent.Captured(razorpayPaymentId))
        paymentRepository.save(payment)
        if (changed) {
            outboxWriter.enqueue("payment.captured", payment.bookingId, outboxPayload("payment.captured", payment))
        }
    }

    @Transactional
    fun processWebhookOnce(event: String, entityId: String, payload: JSONObject): WebhookOutcome {
        // Dedup insert and business update share ONE transaction, deliberately.
        // If applyEvent() below throws, this insert rolls back with it, and
        // Razorpay's next retry (it will retry -- anything but 2xx triggers
        // one) reprocesses the event correctly instead of finding it already
        // "seen" and skipping it forever.
        val isNew = try {
            webhookEventRepository.saveAndFlush(WebhookEvent(event = event, entityId = entityId, payload = payload.toString()))
            true
        } catch (ex: DataIntegrityViolationException) {
            false
        }
        if (!isNew) return WebhookOutcome.DUPLICATE

        val payment = when (event) {
            "payment.captured", "order.paid", "payment.failed" -> paymentRepository.findByRazorpayOrderId(orderIdFrom(payload))
            // Refund payloads nest the payment under payload.payment.entity, not
            // payload.order.entity -- there's no order id on this shape, so the
            // lookup has to go through the payment id instead.
            "refund.processed", "refund.failed" -> paymentRepository.findByRazorpayPaymentId(paymentIdFrom(payload))
            else -> return WebhookOutcome.IGNORED_EVENT
        } ?: return WebhookOutcome.IGNORED_EVENT // unknown order/payment -- log and ack, don't 500

        val changed = when (event) {
            "payment.captured", "order.paid" -> payment.applyEvent(PaymentEvent.Captured(entityId))
            "payment.failed" -> payment.applyEvent(PaymentEvent.Failed(reasonFrom(payload)))
            "refund.processed" -> payment.applyEvent(PaymentEvent.Refunded(reasonFrom(payload)))
            // refund.failed -- the refund attempt didn't go through; the payment
            // stays CAPTURED exactly as it was. No transition, but the dedup
            // insert above still ran, so a retried refund.failed is still safe.
            else -> false
        }
        // Same tx, deliberately -- this INSERT is the entire cost of
        // "publishing" here. PaymentOutboxPublisher is the only thing that
        // ever calls Kafka, on its own schedule, outside any request's
        // transaction (see P8: no network call inside a @Transactional method).
        if (changed && payment.status == PaymentStatus.CAPTURED) {
            outboxWriter.enqueue("payment.captured", payment.bookingId, outboxPayload("payment.captured", payment))
        }
        if (changed && payment.status == PaymentStatus.REFUNDED) {
            outboxWriter.enqueue("payment.refunded", payment.bookingId, outboxPayload("payment.refunded", payment))
        }
        return WebhookOutcome.PROCESSED
    }

}

// order-service and notification both just need to know which booking and
// which payment -- neither gets Razorpay's raw payload, so a schema change
// on Razorpay's side can never break a downstream consumer. "event", not
// "eventType", to match order-service's PaymentEventPayload.@JsonProperty
// mapping on the other side of the wire.
private fun outboxPayload(eventType: String, payment: Payment): String =
    JSONObject(
        mapOf(
            "event" to eventType,
            "bookingId" to payment.bookingId.toString(),
            "paymentId" to payment.id.toString(),
        ),
    ).toString()
enum class WebhookOutcome { PROCESSED, DUPLICATE, IGNORED_EVENT }
class PaymentNotFoundException(bookingId: Uuid) :
    RuntimeException("No payment found for booking $bookingId")

private fun paymentEntity(payload: JSONObject): JSONObject =
    payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity")

private fun refundEntity(payload: JSONObject): JSONObject =
    payload.getJSONObject("payload").getJSONObject("refund").getJSONObject("entity")

private fun orderIdFrom(payload: JSONObject): String = paymentEntity(payload).getString("order_id")

// Refund payloads nest the payment under payload.payment.entity too, so the
// same accessor works for payment.* and refund.* events alike.
private fun paymentIdFrom(payload: JSONObject): String = paymentEntity(payload).getString("id")

private fun reasonFrom(payload: JSONObject): String {
    val inner = payload.getJSONObject("payload")
    if (inner.has("refund")) {
        val refund = refundEntity(payload)
        return refund.optString("notes").ifBlank { "refund ${refund.optString("status", "processed")}" }
    }
    val payment = paymentEntity(payload)
    return payment.optString("error_description")
        .ifBlank { payment.optString("error_reason") }
        .ifBlank { "payment failed" }
}