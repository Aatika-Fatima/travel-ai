package com.travel.payment.internal.repository

import com.travel.payment.internal.entity.PaymentOutboxEntity
import org.springframework.data.repository.CrudRepository
import kotlin.uuid.Uuid

interface PaymentOutboxRepository : CrudRepository<PaymentOutboxEntity, Long> {
    fun findTop50ByStatusOrderByCreatedAtAsc(status: String): List<PaymentOutboxEntity>
    fun findByBookingId(bookingId: Uuid): List<PaymentOutboxEntity>
}
