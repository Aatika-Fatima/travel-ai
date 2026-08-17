package com.travel.notification.internal.worker

import com.travel.common.events.EmailEvent
import com.travel.notification.internal.entity.EmailDeliveryEntity
import com.travel.notification.internal.repository.EmailDeliveryRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.BackOff
import org.springframework.kafka.annotation.DltHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@Component
class EmailWorker(
    private val emailDeliveryRepository: EmailDeliveryRepository,
    private val emailProvider: EmailProvider,
    private val objectMapper: ObjectMapper,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @RetryableTopic(
        attempts = "3",  // 1 original attempt + 2 retries → email-events-retry-0, -retry-1
        backOff = BackOff(delay = 300_000, multiplier = 6.0, maxDelay = 1_800_000),  // ~5 min, then ~30 min
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
    )
    @KafkaListener(topics = ["email-events"], groupId = "email-worker")
    fun onEmailRequested(payload: String) {
        val emailEvent = objectMapper.readValue(payload, EmailEvent::class.java)
        val id = UUID.fromString(emailEvent.emailId)
        if (emailDeliveryRepository.existsByEmailId(id)) return
        val body = "Booking ${emailEvent.bookingReference} has been confirmed."
        emailProvider.send(emailEvent.recipient, "BOOKING CONFIRMED", body)
        emailDeliveryRepository.save(EmailDeliveryEntity(id, "SENT", null, Instant.now()))
        // status fan-out — see G9
        kafkaTemplate.send(
            "email-status-events",
            id.toString(),
            objectMapper.writeValueAsString(mapOf("emailId" to id, "status" to "SENT")),
        )
    }

    @DltHandler
    fun onDeadLettered(payload: String, @Header(KafkaHeaders.EXCEPTION_MESSAGE) reason: String) {
        log.warn("email-events exhausted retries: {} — {}", payload, reason)
        // operators replay from email-events-dlt via Kafka UI once the root cause (e.g. SMTP outage) is fixed
    }
}