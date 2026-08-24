package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.persistence.BookingEntity
import com.travel.common.model.BookingRequest
import kotlin.uuid.Uuid

interface BookingService {
    fun submit(idempotencyKey: String, request: BookingRequest): BookingView
    fun get(bookingId: Uuid): BookingView
    fun cancel(bookingId: Uuid):BookingView
}

fun BookingEntity.toView(justCreated: Boolean) = BookingView(
    bookingId = bookingId,
    status = status,
    offerId = offerId,
    failureReason = failureReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
    justCreated = justCreated,
)