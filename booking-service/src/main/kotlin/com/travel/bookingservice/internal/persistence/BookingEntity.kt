package com.travel.bookingservice.internal.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JavaType
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import kotlin.time.Instant
import kotlin.uuid.Uuid

// See P4 for the full allow-list this enum is deliberately not allowed to
// bypass -- nothing outside BookingStatus.transition() may set this field.
enum class BookingStatus {
    INITIATED, IN_PROGRESS, RESERVED, CONFIRMED,
    CANCELLATION_PENDING, CANCELLED, FAILED, EXPIRED,
}

@Entity
@Table(name = "bookings")
class BookingEntity(
    // Column is "id" in the V1 migration, not the Hibernate-implied
    // "booking_id" -- without this explicit name, ddl-auto: validate fails
    // at startup against the real schema.
    // @JavaType/@JdbcTypeCode, not @Convert -- Hibernate 7 rejects an
    // AttributeConverter on an @Id attribute outright (KotlinUuidJavaType
    // is the Id-safe equivalent of KotlinUuidAttributeConverter, which
    // covers every non-Id Uuid column instead).
    @Id
    @Column(name = "id")
    @JavaType(KotlinUuidJavaType::class)
    @JdbcTypeCode(SqlTypes.UUID)
    var bookingId: Uuid = Uuid.random(),

    // Immutable once written -- see P3's worked example for why a "fix up
    // the idempotency key on an existing row" bug should fail to compile,
    // not fail at runtime. unique = true mirrors uq_bookings_idempotency_key
    // from the migration so this constraint also exists under the
    // Hibernate-generated (ddl-auto: create-drop) schema tests run against.
    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true)
    val idempotencyKey: String,

    @Column(name = "offer_id", nullable = false, updatable = false)
    val offerId: String,

    @Enumerated(EnumType.STRING)
    var status: BookingStatus = BookingStatus.INITIATED,

    @Column(name = "failure_reason")
    var failureReason: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb", updatable = false)
    val requestPayload: String,

    // Every UPDATE checks this against the row it read; a concurrent writer
    // that committed first bumps it, and the loser gets an
    // OptimisticLockException instead of silently overwriting -- see P4.
    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
)
