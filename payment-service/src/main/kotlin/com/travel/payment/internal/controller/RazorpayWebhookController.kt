package com.travel.payment.internal.controller

import com.travel.payment.internal.gateway.razorpay.RazorpayGateway
import com.travel.payment.internal.repository.PaymentTransactionalOps
import com.travel.payment.internal.repository.WebhookOutcome
import org.json.JSONObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class RazorpayWebhookController(val gateway: RazorpayGateway,
    val transactionalOps: PaymentTransactionalOps) {

    @PostMapping("/webhooks/razorpay")
    fun handle(@RequestBody rawBody: String,
               @RequestHeader("X-Razorpay-Signature")signature:String): ResponseEntity<Unit> {
        if(!gateway.verifyWebhookSignature(rawBody,signature)) {
            return ResponseEntity.badRequest().build()
        }

        val payload = JSONObject(rawBody)
        val event = payload.getString("event")
        val entityId = extractEntityId(payload, event)

        val outcome = transactionalOps.processWebhookOnce(event, entityId, payload) // P8
        return when (outcome) {
            WebhookOutcome.PROCESSED, WebhookOutcome.DUPLICATE, WebhookOutcome.IGNORED_EVENT ->
                ResponseEntity.ok().build() // 2xx in all three -- none of these should ever be retried
        }
     }
}

// The dedup key persisted alongside `event` in webhook_events. For payment
// and order events, Razorpay's own payment id is the natural key -- it's
// stable across redelivery and matches what processWebhookOnce looks the
// payment up by. Refund events nest a *separate* refund id, which has to be
// used instead: a payment can have more than one refund, so the payment id
// alone would collide across genuinely distinct refund events.
private fun extractEntityId(payload: JSONObject, event: String): String {
    val inner = payload.getJSONObject("payload")
    return when {
        inner.has("refund") -> inner.getJSONObject("refund").getJSONObject("entity").getString("id")
        inner.has("payment") -> inner.getJSONObject("payment").getJSONObject("entity").getString("id")
        // Events this service doesn't act on (e.g. payment.downtime.*) carry no
        // payment/refund entity at all. processWebhookOnce acks and ignores these
        // regardless of the dedup outcome, so this only needs to not throw --
        // event name + delivery timestamp is a good enough key for that.
        else -> "$event:${payload.optLong("created_at", 0)}"
    }
}