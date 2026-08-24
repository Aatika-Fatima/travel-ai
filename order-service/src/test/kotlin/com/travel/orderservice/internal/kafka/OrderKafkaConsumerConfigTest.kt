package com.travel.orderservice.internal.kafka

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import kotlin.test.assertEquals

class OrderKafkaConsumerConfigTest {
    @Test
    fun `orderManualAckContainerFactory is configured for manual, immediate acknowledgment`() {
        val consumerFactory: ConsumerFactory<String, String> = mock()

        val factory = OrderKafkaConsumerConfig().orderManualAckContainerFactory(consumerFactory)

        assertEquals(ContainerProperties.AckMode.MANUAL_IMMEDIATE, factory.containerProperties.ackMode)
    }
}
