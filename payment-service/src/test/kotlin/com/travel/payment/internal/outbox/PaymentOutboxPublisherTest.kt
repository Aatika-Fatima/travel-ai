package com.travel.payment.internal.outbox

import com.travel.payment.internal.entity.PaymentOutboxEntity
import com.travel.payment.internal.repository.PaymentOutboxRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.core.KafkaTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

class PaymentOutboxPublisherTest {
    private val repository: PaymentOutboxRepository = mock()
    private val kafkaTemplate: KafkaTemplate<String, String> = mock()
    private val publisher = PaymentOutboxPublisher(repository, kafkaTemplate)

    @Test
    fun `publishPending sends each PENDING row to Kafka keyed by booking id and marks it PUBLISHED`() {
        val bookingId = Uuid.random()
        val row = PaymentOutboxEntity(eventType = "payment.captured", bookingId = bookingId, payload = "{}")
        whenever(repository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(listOf(row))

        publisher.publishPending()

        verify(kafkaTemplate).send("payment.events", bookingId.toString(), "{}")
        assertEquals("PUBLISHED", row.status)
        assertNotNull(row.publishedAt)
    }

    @Test
    fun `publishPending does nothing when there are no PENDING rows`() {
        whenever(repository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(emptyList())

        publisher.publishPending()

        verify(kafkaTemplate, never()).send(any(), any(), any())
    }
}
