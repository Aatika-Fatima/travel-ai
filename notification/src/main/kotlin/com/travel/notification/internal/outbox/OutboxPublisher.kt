package com.travel.notification.internal.outbox

import com.travel.notification.internal.repository.BookingOutboxRepository
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OutboxPublisher(private val bookingOutboxRepository: BookingOutboxRepository,
                      private val kafkaTemplate: KafkaTemplate<String, String>) {

    @Transactional
    @Scheduled(fixedDelay = 1_000)
    fun publishPending(){
        val pending = bookingOutboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")
        pending.forEach { row ->
            kafkaTemplate.send("booking-events",row.orderId,row.payload)
            row.status = "PUBLISHED"
            row.publishedAt = Instant.now()

        }
    }
}
