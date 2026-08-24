package com.travel.orderservice.api

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrderStatusTest {
    // Every entry ALLOWED actually declares -- exercised as the "allowed"
    // half of the exhaustive cross-product test below, but spelled out here
    // too so a single broken edge fails with a readable name instead of
    // just "some entry in the cross product failed".
    @Test
    fun `every declared edge in ALLOWED transitions to exactly the state it names`() {
        OrderStatus.ALLOWED.forEach { (key, expected) ->
            val (from, event) = key
            assertEquals(expected, from.transition(event), "transition($from, $event)")
        }
    }

    @Test
    fun `every (status, event) pair not in ALLOWED throws IllegalStateTransitionException`() {
        for (status in OrderStatus.entries) {
            for (event in OrderStatusEvent.entries) {
                val key = status to event
                if (key in OrderStatus.ALLOWED) continue
                assertFailsWith<IllegalStateTransitionException>("transition($status, $event) should have thrown") {
                    status.transition(event)
                }
            }
        }
    }

    @Test
    fun `terminal states accept no events at all`() {
        val terminal = setOf(OrderStatus.PAYMENT_FAILED, OrderStatus.FAILED, OrderStatus.CANCELLED)
        for (status in terminal) {
            for (event in OrderStatusEvent.entries) {
                assertFailsWith<IllegalStateTransitionException> { status.transition(event) }
            }
        }
    }

    @Test
    fun `cancellation is legal from either post-booking state`() {
        assertEquals(OrderStatus.CANCELLED, OrderStatus.CONFIRMED.transition(OrderStatusEvent.CANCEL))
        assertEquals(OrderStatus.CANCELLED, OrderStatus.TICKETED.transition(OrderStatusEvent.CANCEL))
    }

    @Test
    fun `IllegalStateTransitionException message names both the state and the event`() {
        val ex = assertFailsWith<IllegalStateTransitionException> {
            OrderStatus.AWAITING_AIRLINE_CONFIRMATION.transition(OrderStatusEvent.TICKETED)
        }
        assertEquals("Cannot apply TICKETED to an order in state AWAITING_AIRLINE_CONFIRMATION", ex.message)
    }
}
