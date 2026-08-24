package com.travel.payment.internal.repository

import com.travel.payment.internal.entity.Payment
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import kotlin.uuid.Uuid

interface PaymentRepository: JpaRepository<Payment, Uuid> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.idempotencyKey = :idempotencyKey")
    fun findByIdempotencyKey(@Param("idempotencyKey")idempotencyKey: String): Payment?

    fun findByRazorpayOrderId(razorpayOrderId: String): Payment?
    fun findByRazorpayPaymentId(razorpayPaymentId: String): Payment?
    fun findByBookingId(bookingId: Uuid): Payment?
}