package com.travel.bookingservice.internal.outbox

import com.travel.bookingservice.internal.persistence.BookingEventOutboxEntity
import com.travel.bookingservice.internal.persistence.BookingEventOutboxRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class BookingEventOutboxWriterImplTest {
    private val repository: BookingEventOutboxRepository = mock()
    private val writer = BookingEventOutboxWriterImpl(repository)

    @Test
    fun `enqueue saves a PENDING row with the given event type, booking id, and payload`() {
        val bookingId = Uuid.random()

        writer.enqueue("BookingCreated", bookingId, """{"bookingId":"x"}""")

        val captor = argumentCaptor<BookingEventOutboxEntity>()
        verify(repository).save(captor.capture())
        val saved = captor.firstValue
        assertEquals("BookingCreated", saved.eventType)
        assertEquals(bookingId, saved.bookingId)
        assertEquals("""{"bookingId":"x"}""", saved.payload)
        assertEquals("PENDING", saved.status)
        assertNull(saved.publishedAt)
        assertNull(saved.id)
        assertNotNull(saved.createdAt)
    }
}
