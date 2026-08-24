package com.travel.orderservice.internal.kafka

import com.fasterxml.jackson.annotation.JsonProperty

// Matches the fixed payment.events shape from this phase's prerequisite
// callout above — payment-service's outboxPayload() emits the field name
// "event", not "eventType", so the JSON key has to be mapped explicitly
// rather than relying on the property name to match it.
data class PaymentEventPayload(
    @JsonProperty("event")
    val eventType: String,
    val bookingId: String,
    val paymentId: String,
)