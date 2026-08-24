package com.travel.payment.internal.outbox

import com.travel.payment.internal.entity.PaymentOutboxEntity
import com.travel.payment.internal.repository.PaymentOutboxRepository
import org.springframework.stereotype.Component
import kotlin.uuid.Uuid

@Component
class PaymentOutboxWriterImpl(
    private val repository: PaymentOutboxRepository,
) : PaymentOutboxWriter {
    override fun enqueue(eventType: String, bookingId: Uuid, payload: String) {
        repository.save(PaymentOutboxEntity(eventType = eventType, bookingId = bookingId, payload = payload))
    }
}
