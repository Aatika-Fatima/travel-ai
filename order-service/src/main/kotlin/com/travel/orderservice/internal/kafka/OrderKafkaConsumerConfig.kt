package com.travel.orderservice.internal.kafka

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE

// MANUAL_IMMEDIATE -- not the default auto-commit -- is what makes
// at-least-once actually true here. Auto-commit acknowledges on a timer
// regardless of whether submit() succeeded; a crash between that
// auto-commit and a completed insert would silently lose the booking.
//
// Named Order*, not just KafkaConsumerConfig -- booking-service's own
// internal.kafka.KafkaConsumerConfig has that exact simple name, and both
// land in the same app process's component scan. A shared name would
// collide on Spring's default bean naming (decapitalized simple class
// name) across the two modules.
@Configuration
class OrderKafkaConsumerConfig {
    @Bean
    fun orderManualAckContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConsumerFactory(consumerFactory)
            containerProperties.ackMode = MANUAL_IMMEDIATE
        }
}
