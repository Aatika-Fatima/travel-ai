package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.persistence.BookingStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookingStatusEventTest {
    // Every entry ALLOWED actually declares -- exercised as the "allowed"
    // half of the exhaustive cross-product test below, but spelled out here
    // too so a single broken edge fails with a readable name instead of
    // just "some entry in the cross product failed".
    @Test
    fun `every declared edge in ALLOWED transitions to exactly the state it names`() {
        ALLOWED.forEach { (key, expected) ->
            val (from, event) = key
            assertEquals(expected, from.transition(event), "transition($from, $event)")
        }
    }

    @Test
    fun `every (status, event) pair not in ALLOWED throws IllegalStateTransitionException`() {
        for (status in BookingStatus.entries) {
            for (event in BookingStatusEvent.entries) {
                val key = status to event
                if (key in ALLOWED) continue
                assertFailsWith<IllegalStateTransitionException>("transition($status, $event) should have thrown") {
                    status.transition(event)
                }
            }
        }
    }

    @Test
    fun `terminal states accept no events at all`() {
        val terminal = setOf(BookingStatus.CANCELLED, BookingStatus.FAILED, BookingStatus.EXPIRED)
        for (status in terminal) {
            for (event in BookingStatusEvent.entries) {
                assertFailsWith<IllegalStateTransitionException> { status.transition(event) }
            }
        }
    }

    @Test
    fun `IllegalStateTransitionException message names both the state and the event`() {
        val ex = assertFailsWith<IllegalStateTransitionException> {
            BookingStatus.CANCELLED.transition(BookingStatusEvent.PUBLISHED)
        }
        assertEquals("Cannot apply PUBLISHED to a booking in state CANCELLED", ex.message)
    }
}
