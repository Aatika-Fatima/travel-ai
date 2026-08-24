package com.travel.payment.internal.outbox

import kotlin.uuid.Uuid

interface PaymentOutboxWriter {
    fun enqueue(eventType: String, bookingId: Uuid, payload: String)
}
