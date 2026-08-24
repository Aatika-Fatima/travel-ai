package com.travel.orderservice.internal.persistence

import com.travel.orderservice.api.OrderStatus
import com.travel.orderservice.internal.kafka.CustomerPaymentStatus
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
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity
@Table(name = "orders")
class OrderEntity(
    // @JavaType/@JdbcTypeCode, not @Convert -- Hibernate 7 rejects an
    // AttributeConverter on an @Id attribute outright (KotlinUuidJavaType
    // is the Id-safe equivalent of KotlinUuidAttributeConverter, which
    // covers every non-Id Uuid column instead).
    @Id
    @JavaType(KotlinUuidJavaType::class)
    @JdbcTypeCode(SqlTypes.UUID)
    val id: Uuid = Uuid.random(),

    // Immutable once written -- see P3's worked example for why a "fix up
    // the idempotency key on an existing row" bug should fail to compile,
    // not fail at runtime. unique = true mirrors uq_orders_idempotency_key
    // from the migration so this constraint also exists under the
    // Hibernate-generated (ddl-auto: create-drop) schema tests run against.
    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true)
    val idempotencyKey: String,

    @Column(name = "offer_id", nullable = false, updatable = false)
    val offerId: String,

    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.PENDING_SUBMISSION,

    @Column(name = "duffel_order_id")
    var duffelOrderId: String? = null,

    @Column(name = "booking_reference")
    var bookingReference: String? = null,

    // Independent of `status` (Duffel/airline fulfillment) -- see P12 for
    // why this is a second field rather than a new edge in OrderStatus's
    // transition map.
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_payment_status", nullable = false)
    var customerPaymentStatus: CustomerPaymentStatus = CustomerPaymentStatus.AWAITING_PAYMENT,

    // Every UPDATE checks this against the row it read; a concurrent writer
    // that committed first bumps it, and the loser gets an
    // OptimisticLockException instead of silently overwriting -- see P5.
    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Clock.System.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Clock.System.now(),
)
