package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.persistence.BookingStatus
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class BookingView(
    val bookingId: Uuid,
    val status: BookingStatus,
    val offerId: String,
    val failureReason: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val justCreated: Boolean
    )
