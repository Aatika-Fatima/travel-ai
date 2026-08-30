package com.travel.orderservice.api

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class OrderView(
    val orderId: Uuid,
    val status: OrderStatus,
    val offerId: String,
    val duffelOrderId: String?,
    val bookingReference: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    // Populated only for FAILED orders -- the reason Duffel rejected it.
    val failureReason: String? = null,
    // Lets OrderController below answer 201 vs 200 — see its step-note.
    // Defaults to false so the two duplicate-check branches in
    // OrderServiceImpl.submit(), which only ever look up an *existing*
    // row, don't need to pass it explicitly.
    val justCreated: Boolean = false,
)