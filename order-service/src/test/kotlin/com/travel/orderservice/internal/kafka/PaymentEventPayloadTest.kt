package com.travel.orderservice.internal.kafka

import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals

class PaymentEventPayloadTest {
    private val mapper = jacksonObjectMapper()

    // payment-service's outboxPayload() emits the field name "event", not
    // "eventType" -- this is the contract test the module's own step-note
    // asks for: prove the JSON key maps onto the Kotlin property via
    // @JsonProperty, not by property-name coincidence.
    @Test
    fun `deserializes the wire field "event" onto the eventType property`() {
        val json = """{"event":"payment.captured","bookingId":"b1","paymentId":"p1"}"""

        val payload = mapper.readValue(json, PaymentEventPayload::class.java)

        assertEquals("payment.captured", payload.eventType)
        assertEquals("b1", payload.bookingId)
        assertEquals("p1", payload.paymentId)
    }
}
