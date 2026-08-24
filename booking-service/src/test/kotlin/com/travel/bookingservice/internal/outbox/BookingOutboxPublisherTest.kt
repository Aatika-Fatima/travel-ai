package com.travel.bookingservice.internal.outbox

import com.travel.bookingservice.internal.persistence.BookingEventOutboxEntity
import com.travel.bookingservice.internal.persistence.BookingEventOutboxRepository
import com.travel.bookingservice.internal.persistence.BookingStatus
import com.travel.bookingservice.internal.service.BookingStatusEvent
import com.travel.bookingservice.internal.service.BookingTransactionalOps
import com.travel.bookingservice.internal.service.IllegalStateTransitionException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.core.KafkaTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

class BookingOutboxPublisherTest {
    private val repository: BookingEventOutboxRepository = mock()
    private val kafkaTemplate: KafkaTemplate<String, String> = mock()
    private val transactionalOps: BookingTransactionalOps = mock()
    private val publisher = BookingOutboxPublisher(repository, kafkaTemplate, transactionalOps)

    private fun row(eventType: String, bookingId: Uuid = Uuid.random()) =
        BookingEventOutboxEntity(bookingId = bookingId, eventType = eventType, payload = "{}")

    @Test
    fun `publishPending does nothing when there are no PENDING rows`() {
        whenever(repository.claimPending()).thenReturn(emptyList())

        publisher.publishPending()

        verify(kafkaTemplate, never()).send(any(), any(), any())
        verify(transactionalOps, never()).advance(any(), any(), anyOrNull())
    }

    @Test
    fun `publishPending sends a BookingCreated row to Kafka, marks it PUBLISHED, and advances the booking`() {
        val bookingRow = row("BookingCreated")
        whenever(repository.claimPending()).thenReturn(listOf(bookingRow))

        publisher.publishPending()

        verify(kafkaTemplate).send("booking.events", bookingRow.bookingId.toString(), "{}")
        assertEquals("PUBLISHED", bookingRow.status)
        assertNotNull(bookingRow.publishedAt)
        verify(transactionalOps).advance(bookingRow.bookingId, BookingStatusEvent.PUBLISHED)
    }

    @Test
    fun `publishPending marks a BookingCancelled row PUBLISHED without ever advancing the booking`() {
        val cancelRow = row("BookingCancelled")
        whenever(repository.claimPending()).thenReturn(listOf(cancelRow))

        publisher.publishPending()

        verify(kafkaTemplate).send("booking.events", cancelRow.bookingId.toString(), "{}")
        assertEquals("PUBLISHED", cancelRow.status)
        verify(transactionalOps, never()).advance(any(), any(), anyOrNull())
    }

    @Test
    fun `publishPending swallows a race where the booking moved on before PUBLISHED could apply`() {
        val bookingRow = row("BookingCreated")
        whenever(repository.claimPending()).thenReturn(listOf(bookingRow))
        whenever(transactionalOps.advance(bookingRow.bookingId, BookingStatusEvent.PUBLISHED))
            .thenThrow(IllegalStateTransitionException(BookingStatus.CANCELLED, BookingStatusEvent.PUBLISHED))

        publisher.publishPending()

        // The row still reached Kafka and PUBLISHED -- only the booking's own
        // status transition was skipped, not the relay of the event itself.
        assertEquals("PUBLISHED", bookingRow.status)
    }

    @Test
    fun `publishPending processes every claimed row, not just the first`() {
        val first = row("BookingCreated")
        val second = row("BookingCreated")
        whenever(repository.claimPending()).thenReturn(listOf(first, second))

        publisher.publishPending()

        verify(transactionalOps).advance(first.bookingId, BookingStatusEvent.PUBLISHED)
        verify(transactionalOps).advance(second.bookingId, BookingStatusEvent.PUBLISHED)
    }
}
