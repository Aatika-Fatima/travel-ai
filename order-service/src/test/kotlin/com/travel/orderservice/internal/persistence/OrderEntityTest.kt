package com.travel.orderservice.internal.persistence

import com.travel.orderservice.api.OrderStatus
import com.travel.orderservice.internal.kafka.CustomerPaymentStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

class OrderEntityTest {
    // Every other test in this module builds an OrderEntity through the
    // default-value constructor (a handful of named args, everything else
    // defaulted). This is the one place every constructor parameter is
    // supplied explicitly, so the fully-explicit constructor overload --
    // and every field it assigns -- actually runs at least once.
    @Test
    fun `every field round-trips through the fully-explicit constructor`() {
        val id = Uuid.random()
        val created = Instant.fromEpochMilliseconds(1_000)
        val updated = Instant.fromEpochMilliseconds(2_000)

        val entity =
            OrderEntity(
                id,
                "idem-1",
                "off_1",
                OrderStatus.CONFIRMED,
                "duf_order_1",
                "ABC123",
                CustomerPaymentStatus.PAID,
                3L,
                created,
                updated,
            )

        assertEquals(id, entity.id)
        assertEquals("idem-1", entity.idempotencyKey)
        assertEquals("off_1", entity.offerId)
        assertEquals(OrderStatus.CONFIRMED, entity.status)
        assertEquals("duf_order_1", entity.duffelOrderId)
        assertEquals("ABC123", entity.bookingReference)
        assertEquals(CustomerPaymentStatus.PAID, entity.customerPaymentStatus)
        assertEquals(3L, entity.version)
        assertEquals(created, entity.createdAt)
        assertEquals(updated, entity.updatedAt)
    }

    @Test
    fun `defaults produce a fresh PENDING_SUBMISSION, unpaid order with a random id`() {
        val entity = OrderEntity(idempotencyKey = "idem-2", offerId = "off_2")

        assertEquals(OrderStatus.PENDING_SUBMISSION, entity.status)
        assertEquals(CustomerPaymentStatus.AWAITING_PAYMENT, entity.customerPaymentStatus)
        assertEquals(0L, entity.version)
        assertEquals(null, entity.duffelOrderId)
        assertEquals(null, entity.bookingReference)
    }
}
