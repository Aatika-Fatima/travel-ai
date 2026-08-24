package com.travel.bookingservice.internal.persistence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface BookingRepository : JpaRepository<BookingEntity, Uuid> {
    // PESSIMISTIC_WRITE -- see P2 for exactly which race this does, and
    // does not, protect against. Copied 1:1 from PaymentRepository in
    // payment-service, down to the @Lock/@Query shape.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BookingEntity b where b.idempotencyKey = :idempotencyKey")
    fun findByIdempotencyKey(@Param("idempotencyKey") idempotencyKey: String): BookingEntity?
    fun findByBookingId(@Param("bookingId")bookingId: Uuid): BookingEntity?

    // Added for the expiry sweep -- a plain, unlocked scan. No pessimistic
    // lock here either, for the same reason cancel() doesn't take one:
    // advance()'s optimistic check on the write is what's actually safe,
    // not locking the read.
    @Query("select b from BookingEntity b where b.status in :statuses and b.updatedAt < :cutoff")
    fun findStaleActive(
        @Param("statuses") statuses: Set<BookingStatus>,
        @Param("cutoff") cutoff: Instant,
    ): List<BookingEntity>
}
