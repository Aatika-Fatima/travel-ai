package com.travel.bookingservice.internal.outbox

import com.travel.bookingservice.internal.persistence.BookingEventOutboxEntity
import com.travel.bookingservice.internal.persistence.BookingEventOutboxRepository
import org.springframework.stereotype.Component
import kotlin.uuid.Uuid

// Named BookingEventOutbox*, not just BookingOutbox* -- notification's own
// internal.outbox.BookingOutboxWriterImpl (its own, unrelated outbox) has
// that exact simple name, and both land in the same app process's
// component scan. A shared name would collide on Spring's default bean
// naming (decapitalized simple class name) across the two modules.
@Component
class BookingEventOutboxWriterImpl(
    private val repository: BookingEventOutboxRepository,
) : BookingEventOutboxWriter {
    // No @Transactional here -- enqueue() always runs as part of a caller's
    // own transaction (findOrInsertPending in P3, advance() in P4). Adding
    // one here would silently start a *nested* transaction context instead
    // of joining the caller's, defeating the whole "atomic with the write"
    // point of the outbox pattern.
    override fun enqueue(eventType: String, bookingId: Uuid, payload: String) {
        repository.save(BookingEventOutboxEntity(bookingId = bookingId, eventType = eventType, payload = payload))
    }
}