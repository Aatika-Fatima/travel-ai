package com.travel.payment.internal.outbox

import com.travel.payment.internal.repository.PaymentOutboxRepository
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class PaymentOutboxPublisher(
    private val repository: PaymentOutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    @Transactional
    @Scheduled(fixedDelay = 1_000)
    fun publishPending() {
        repository.findTop50ByStatusOrderByCreatedAtAsc("PENDING").forEach { row ->
            // bookingId as the Kafka key -- every event for one booking lands
            // on the same partition, so a consumer sees them in write order.
            kafkaTemplate.send("payment.events", row.bookingId.toString(), row.payload)
            row.status = "PUBLISHED"
            row.publishedAt = Instant.now()
        }
    }
}
