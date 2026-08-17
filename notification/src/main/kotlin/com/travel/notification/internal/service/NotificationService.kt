package com.travel.notification.internal.service

import com.travel.common.events.BookingEvent
import com.travel.common.events.EmailEvent
import com.travel.notification.internal.entity.NotificationRequestEntity
import com.travel.notification.internal.repository.NotificationRequestRepository
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class NotificationService(
    private val notificationRequestRepository: NotificationRequestRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    fun handleBookingConfirmed(bookingEventJson:String){
        val bookingEvent: BookingEvent = objectMapper.readValue(bookingEventJson, BookingEvent::class.java)

        bookingEvent.emails.distinct().forEach { recipientEmail ->
            val notificationId = UUID.randomUUID()

            notificationRequestRepository.save(
                NotificationRequestEntity(notificationId,"EMAIL","BOOKING_CONFIRMED",
                    recipientEmail)
            )

            val emailEvent = EmailEvent(notificationId.toString(),
                "BOOKING_CONFIRMED", recipientEmail, bookingEvent.bookingReference)
            kafkaTemplate.send("email-events",notificationId.toString(),objectMapper.writeValueAsString(emailEvent))
        }
    }
}