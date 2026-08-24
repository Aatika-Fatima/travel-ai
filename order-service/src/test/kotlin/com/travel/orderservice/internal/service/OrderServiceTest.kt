package com.travel.orderservice.internal.service

import com.travel.orderservice.api.OrderStatus
import com.travel.orderservice.internal.persistence.OrderEntity
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderServiceTest {
    private fun entity() =
        OrderEntity(
            idempotencyKey = "idem-1",
            offerId = "off_1",
            status = OrderStatus.CONFIRMED,
            duffelOrderId = "duf_order_1",
            bookingReference = "ABC123",
        )

    @Test
    fun `toView maps every OrderEntity field onto OrderView`() {
        val entity = entity()

        val view = entity.toView()

        assertEquals(entity.id, view.orderId)
        assertEquals(entity.status, view.status)
        assertEquals(entity.offerId, view.offerId)
        assertEquals(entity.duffelOrderId, view.duffelOrderId)
        assertEquals(entity.bookingReference, view.bookingReference)
        assertEquals(entity.createdAt, view.createdAt)
        assertEquals(entity.updatedAt, view.updatedAt)
        assertFalse(view.justCreated)
    }

    @Test
    fun `toView carries justCreated through when supplied`() {
        assertTrue(entity().toView(justCreated = true).justCreated)
    }
}
