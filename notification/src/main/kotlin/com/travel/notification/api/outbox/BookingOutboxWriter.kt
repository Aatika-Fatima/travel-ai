package com.travel.notification.api.outbox

interface BookingOutboxWriter {
    fun enqueue(
        eventType: String,
        orderId: String,
        payload: String,
    )
}
