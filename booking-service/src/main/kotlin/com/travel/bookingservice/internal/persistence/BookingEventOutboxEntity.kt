package com.travel.bookingservice.internal.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

// Same shape as PaymentOutboxEntity in payment-service, down to the
// BIGSERIAL id and the nullable publishedAt. Table is booking_event_outbox,
// not booking_outbox -- notification's own migration already claimed that
// table name in this shared database for its own, unrelated outbox.
@Entity
@Table(name = "booking_event_outbox")
class BookingEventOutboxEntity(
    @Column(name = "booking_id", nullable = false) val bookingId: Uuid,
    @Column(name = "event_type", nullable = false, length = 64) val eventType: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val payload: String,

    @Column(nullable = false, length = 16) var status: String = "PENDING",
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Clock.System.now(),
    @Column(name = "published_at") var publishedAt: Instant? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}