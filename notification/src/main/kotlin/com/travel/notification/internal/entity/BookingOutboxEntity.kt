package com.travel.notification.internal.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(
    name = "booking_outbox",
    uniqueConstraints = [
        UniqueConstraint(name = "ux_booking_outbox_event_id", columnNames = ["event_id"]),
    ],
)
class BookingOutboxEntity(
    @Column(name = "event_id", nullable = false)
    var eventId: java.util.UUID,
    @Column(name = "event_type", nullable = false, length = 64)
    var eventType: String,
    @Column(name = "order_id", nullable = false, length = 64)
    var orderId: String,
    @Column(name = "payload", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    var payload: String,
    @Column(name = "status", nullable = false, length = 16)
    var status: String = "PENDING",
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "published_at")
    var publishedAt: Instant? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}