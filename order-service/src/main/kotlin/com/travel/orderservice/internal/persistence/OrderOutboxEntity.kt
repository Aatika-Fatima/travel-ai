package com.travel.orderservice.internal.persistence

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

// Maps the order_outbox table from V11 (P1) — published_at IS NULL is the
// "still pending" predicate, not a separate status column (contrast
// booking-service's booking_event_outbox, which uses one).
@Entity
@Table(name = "order_outbox")
class OrderOutboxEntity(
    @Column(name = "aggregate_id", nullable = false) val aggregateId: Uuid,
    @Column(name = "event_type", nullable = false, length = 64) val eventType: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val payload: String,

    @Column(name = "created_at", nullable = false) val createdAt: Instant = Clock.System.now(),
    @Column(name = "published_at") var publishedAt: Instant? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}