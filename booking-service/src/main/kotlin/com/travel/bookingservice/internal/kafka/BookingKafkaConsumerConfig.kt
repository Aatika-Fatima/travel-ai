package com.travel.bookingservice.internal.kafka

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

// Named Booking*, not just KafkaConsumerConfig -- order-service's own
// internal.kafka.KafkaConsumerConfig (its own, unrelated container-factory
// config) has that exact simple name, and both land in the same app
// process's component scan. A shared name would collide on Spring's
// default bean naming (decapitalized simple class name) across the two
// modules, same as the BookingEventOutboxWriter* naming note.
@Configuration
class BookingKafkaConsumerConfig {
    // MANUAL_IMMEDIATE, exactly like order_service.html §P11 -- auto-commit
    // would acknowledge on a timer regardless of whether advance() actually
    // committed, which is precisely the gap that would let a crash between
    // "Kafka delivered this" and "the transition landed" silently drop it.
    @Bean
    fun bookingManualAckContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConsumerFactory(consumerFactory)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        }
}
