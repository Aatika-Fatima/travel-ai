package com.travel.payment.internal.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import kotlin.uuid.Uuid

@Entity
@Table(name = "payment_outbox")
class PaymentOutboxEntity(
    @Column(name = "event_id", nullable = false) val eventId: Uuid = Uuid.random(),
    @Column(name = "event_type", nullable = false, length = 64) val eventType: String,
    @Column(name = "booking_id", nullable = false) val bookingId: Uuid,

    // Downstream-facing event body -- never Razorpay's raw webhook payload;
    // that shape isn't this service's to hand out to consumers.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val payload: String,

    @Column(nullable = false, length = 16) var status: String = "PENDING",
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
    @Column(name = "published_at") var publishedAt: Instant? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
