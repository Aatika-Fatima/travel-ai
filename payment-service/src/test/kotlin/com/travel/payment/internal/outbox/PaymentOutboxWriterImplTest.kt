package com.travel.payment.internal.outbox

import com.travel.payment.internal.entity.PaymentOutboxEntity
import com.travel.payment.internal.repository.PaymentOutboxRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class PaymentOutboxWriterImplTest {
    private val repository: PaymentOutboxRepository = mock()
    private val writer = PaymentOutboxWriterImpl(repository)

    @Test
    fun `enqueue saves a PENDING row with the given event type, booking id, and payload`() {
        val bookingId = Uuid.random()

        writer.enqueue("payment.captured", bookingId, """{"bookingId":"x"}""")

        val captor = argumentCaptor<PaymentOutboxEntity>()
        verify(repository).save(captor.capture())
        val saved = captor.firstValue
        assertEquals("payment.captured", saved.eventType)
        assertEquals(bookingId, saved.bookingId)
        assertEquals("""{"bookingId":"x"}""", saved.payload)
        assertEquals("PENDING", saved.status)
        assertNull(saved.publishedAt)
        assertNull(saved.id)
        assertNotNull(saved.eventId)
        assertNotNull(saved.createdAt)

        // id is DB-assigned (IDENTITY) -- Hibernate sets it via reflection
        // after insert, which this unit test doesn't go through, so exercise
        // the setter directly.
        saved.id = 42L
        assertEquals(42L, saved.id)
    }
}
