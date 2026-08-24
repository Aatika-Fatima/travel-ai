package com.travel.payment.internal.entity

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
import java.time.Instant
import kotlin.uuid.Uuid

@Entity
@Table(name = "payments")
class Payment(
    // @JavaType/@JdbcTypeCode, not @Convert -- Hibernate 7 rejects an
    // AttributeConverter on an @Id attribute outright (KotlinUuidJavaType
    // is the Id-safe equivalent of KotlinUuidAttributeConverter, which
    // covers bookingId below and every other non-Id Uuid column instead).
    @Id
    @JavaType(KotlinUuidJavaType::class)
    @JdbcTypeCode(SqlTypes.UUID)
    val id: Uuid = Uuid.random(),
    @Column(name = "booking_id") val bookingId: Uuid,
    @Column(name = "idempotency_key") val idempotencyKey: String,
    @Column(name = "amount_paise") val amountPaise: Long,
    val currency: String,

    @Column(name = "razorpay_order_id") var razorpayOrderId: String? = null,
    @Column(name = "razorpay_payment_id") var razorpayPaymentId: String? = null,

    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.CREATED,

    @Column(name = "failure_reason") var failureReason: String? = null,

    // Every UPDATE checks this against the row it read; a concurrent writer
    // that committed first bumps it, and the loser gets an
    // OptimisticLockException instead of silently overwriting — see P5/P6.
    @Version var version: Long = 0,

    @Column(name = "created_at") val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
)