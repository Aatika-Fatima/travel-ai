package com.travel.bookingservice.internal.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

class BookingEntityTest {
    // Every other test in this module builds a BookingEntity through the
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
            BookingEntity(
                id,
                "bk-1",
                "off_1",
                BookingStatus.FAILED,
                "card_declined",
                "{}",
                3L,
                created,
                updated,
            )

        assertEquals(id, entity.bookingId)
        assertEquals("bk-1", entity.idempotencyKey)
        assertEquals("off_1", entity.offerId)
        assertEquals(BookingStatus.FAILED, entity.status)
        assertEquals("card_declined", entity.failureReason)
        assertEquals("{}", entity.requestPayload)
        assertEquals(3L, entity.version)
        assertEquals(created, entity.createdAt)
        assertEquals(updated, entity.updatedAt)
    }
}
