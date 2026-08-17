package com.travel.notification.internal.outbox

import com.travel.notification.api.outbox.BookingOutboxWriter
import com.travel.notification.internal.entity.BookingOutboxEntity
import com.travel.notification.internal.repository.BookingOutboxRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BookingOutboxWriterImpl(
    private val bookingOutboxRepository: BookingOutboxRepository,
) : BookingOutboxWriter {
    override fun enqueue(
        eventType: String,
        orderId: String,
        payload: String,
    ) {
        bookingOutboxRepository.save(
            BookingOutboxEntity(
                eventId = UUID.randomUUID(),
                eventType = eventType,
                orderId = orderId,
                payload = payload,
            ),
        )
    }
}
