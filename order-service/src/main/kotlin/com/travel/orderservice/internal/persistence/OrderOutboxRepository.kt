package com.travel.orderservice.internal.persistence

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import kotlin.time.Instant

interface OrderOutboxRepository : JpaRepository<OrderOutboxEntity, Long> {

    // PESSIMISTIC_WRITE + lock.timeout = -2 is Hibernate's way of asking for
    // FOR UPDATE SKIP LOCKED — same trick as BookingEventOutboxRepository
    // (booking_service.html §P5). A second relay instance polling at the
    // same moment skips rows this one already has, instead of blocking.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    fun findByPublishedAtIsNullOrderByCreatedAtAsc(pageable: Pageable): List<OrderOutboxEntity>

    // Thin wrapper so OutboxRelay.relay() below can call lockNextBatch(limit
    // = 100) directly, the way it's already written.
    fun lockNextBatch(limit: Int): List<OrderOutboxEntity> =
        findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, limit))

    // Bound as a parameter, not HQL's CURRENT_TIMESTAMP -- Hibernate infers
    // CURRENT_TIMESTAMP as java.sql.Timestamp, which it then refuses to
    // assign to a kotlin.time.Instant-typed path in an UPDATE ... SET.
    // clearAutomatically -- a bulk HQL update bypasses the persistence
    // context entirely, so without this a subsequent read in the same
    // transaction would return the stale, still-managed pre-update entity
    // instead of re-fetching the row this statement just changed.
    @Modifying(clearAutomatically = true)
    @Query("update OrderOutboxEntity o set o.publishedAt = :publishedAt where o.id = :id")
    fun markPublished(@Param("id") id: Long, @Param("publishedAt") publishedAt: Instant)

    // Feeds OrderMetrics.recordOutboxBacklog() (P9) -- is the relay keeping up.
    fun countByPublishedAtIsNull(): Long
}