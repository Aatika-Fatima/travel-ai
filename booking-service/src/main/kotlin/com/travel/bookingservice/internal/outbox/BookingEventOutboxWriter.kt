package com.travel.bookingservice.internal.outbox

import kotlin.uuid.Uuid

interface BookingEventOutboxWriter {
    fun enqueue(eventType: String, bookingId: Uuid, payload: String)
}