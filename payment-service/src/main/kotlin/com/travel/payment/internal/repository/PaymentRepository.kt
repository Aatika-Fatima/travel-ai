package com.travel.payment.internal.repository

import com.travel.payment.internal.entity.Payment
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

interface PaymentRepository: CrudRepository<Payment, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id in (:idempotencyKey)")
    fun findByIdempotencyKey(@Param("idempotencyKey")idempotencyKey: String): Payment?

    fun findById(@Param("id")id: String): Payment?

    fun findByRazoryPayOrderId(razoryPayOrderId: UUID): Payment?
    fun findByRazorPayPaymentId(razorPayBookingId: UUID): Payment?
}