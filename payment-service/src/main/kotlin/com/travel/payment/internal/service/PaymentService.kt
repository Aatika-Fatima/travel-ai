package com.travel.payment.internal.service

import com.travel.payment.internal.entity.Payment
import kotlin.uuid.Uuid

interface PaymentService {
      fun createOrder( idempotencyKey: String,bookingId: Uuid, amountPaise: Long, currency: String): Payment
}