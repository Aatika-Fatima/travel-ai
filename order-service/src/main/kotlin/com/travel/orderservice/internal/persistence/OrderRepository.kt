package com.travel.orderservice.internal.persistence

import com.travel.orderservice.api.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import kotlin.time.Instant
import kotlin.uuid.Uuid

// No @Lock here -- P2 explains why uq_orders_idempotency_key alone is
// enough: a plain read backs the pre-check in submit(), and the unique
// constraint (not a lock) is what actually decides between two racing
// inserts.
interface OrderRepository : JpaRepository<OrderEntity, Uuid> {
    fun findByIdempotencyKey(idempotencyKey: String): OrderEntity?

    // Backs ReconciliationJob.sweep() (P8) -- every order P6 left in
    // AWAITING_AIRLINE_CONFIRMATION for longer than the sweep's own cutoff.
    fun findByStatusAndUpdatedAtBefore(status: OrderStatus, cutoff: Instant): List<OrderEntity>
}
