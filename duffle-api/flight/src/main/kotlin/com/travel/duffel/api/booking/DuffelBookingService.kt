package com.travel.duffel.api.booking

import com.travel.common.model.BookingRequest
import com.travel.common.model.BookingResult

interface DuffelBookingService {
    // idempotencyKey now supplied by the caller — order-service owns whether
    // this is a first attempt or a replay; the gateway no longer decides.
    // orderId is optional -- order-service passes its own order id so it can
    // be stamped as metadata.internal_order_id on the Duffel order (§P8);
    // callers outside order-service's saga (e.g. the legacy /flights/book
    // endpoint) have no order id to correlate back to and omit it.
    fun createBooking(request: BookingRequest, idempotencyKey: String, orderId: String? = null): BookingResult
}
