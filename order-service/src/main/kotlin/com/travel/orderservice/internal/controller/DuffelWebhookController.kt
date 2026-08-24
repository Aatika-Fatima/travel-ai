package com.travel.orderservice.internal.controller

import com.travel.orderservice.api.OrderStatusEvent
import com.travel.orderservice.internal.service.OrderTransitions
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.uuid.Uuid

@RestController
class DuffelWebhookController(
    private val transitions: OrderTransitions,
    @Value("\${duffel.webhook-secret}") private val webhookSecret: String,
) {
    private val mapper = jacksonObjectMapper()

    // Only order.created is handled -- every other outcome is left to the
    // reconciliation sweep (ReconciliationJob), per §P8's own "why this
    // can't be just retry later" note.
    @PostMapping("/webhooks/duffel")
    fun handle(
        @RequestBody rawBody: String,
        @RequestHeader("Duffel-Signature") signatureHeader: String,
    ): ResponseEntity<Unit> {
        if (!verify(signatureHeader, rawBody, webhookSecret)) {
            // 401, not 400 -- an unverified body is never "malformed", it's
            // untrusted, and Duffel's own retry behavior treats the two the
            // same.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val event = mapper.readTree(rawBody)
        if (event["type"]?.asString() != "order.created") {
            // Ack anyway -- an unhandled event type isn't an error, and NOT
            // acking it just makes Duffel retry a webhook this phase was
            // never going to act on.
            return ResponseEntity.ok().build()
        }

        // Reads metadata.internal_order_id, not Duffel's own order id --
        // this webhook is exactly what lets order-service learn Duffel's
        // order id for the first time on a 202 flow, so correlating by
        // *our* id (stamped on the order-creation call, see duffle-api's
        // DuffelOrderRequestPayload.metadata) is the only key guaranteed to
        // already exist on both sides.
        val orderId = Uuid.parse(event["data"]["metadata"]["internal_order_id"].asString())
        transitions.advance(orderId, OrderStatusEvent.WEBHOOK_CONFIRMED)
        return ResponseEntity.ok().build()
    }
}
