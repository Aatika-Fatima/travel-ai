package com.travel.notification.internal.consumer

import com.travel.notification.internal.service.NotificationService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class BookingEventConsumer(
    private val notificationService: NotificationService,
) {
    @KafkaListener(topics = ["booking-events"], groupId = "notification-service")
    fun onBookingConfirmed(payload: String) {
        notificationService.handleBookingConfirmed(payload)
    }
}
