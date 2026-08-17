package com.travel.notification.internal.repository

import com.travel.notification.internal.entity.BookingOutboxEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

interface BookingOutboxRepository : CrudRepository<BookingOutboxEntity, Long>{
    fun findTop50ByStatusOrderByCreatedAtAsc(@Param("status")status: String):List<BookingOutboxEntity>
}