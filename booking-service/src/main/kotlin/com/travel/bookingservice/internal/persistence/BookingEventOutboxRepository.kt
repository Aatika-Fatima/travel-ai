package com.travel.bookingservice.internal.persistence

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints

interface BookingEventOutboxRepository: JpaRepository<BookingEventOutboxEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")) // -2 = SKIP LOCKED
    @Query("select o from BookingEventOutboxEntity o where o.status = 'PENDING' order by o.createdAt asc limit 50")
    fun claimPending():List<BookingEventOutboxEntity>
}