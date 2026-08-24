package com.travel.orderservice.internal.outbox

import com.travel.orderservice.internal.persistence.OrderOutboxEntity
import com.travel.orderservice.internal.persistence.OrderOutboxRepository
import com.travel.orderservice.internal.service.OutboxEvent
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class OutboxWriterImplTest {
    private val repository: OrderOutboxRepository = mock()
    private val writer = OutboxWriterImpl(repository)

    @Test
    fun `append saves a row with the given aggregate id, event type, and payload`() {
        val orderId = Uuid.random()
        val event = OutboxEvent(eventType = "OrderConfirmed", payload = """{"orderId":"x"}""")

        writer.append(orderId, event)

        val captor = argumentCaptor<OrderOutboxEntity>()
        verify(repository).save(captor.capture())
        val saved = captor.firstValue
        assertEquals(orderId, saved.aggregateId)
        assertEquals("OrderConfirmed", saved.eventType)
        assertEquals("""{"orderId":"x"}""", saved.payload)
        assertNull(saved.publishedAt)
        assertNull(saved.id)
        assertNotNull(saved.createdAt)
    }
}
