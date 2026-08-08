package com.travel.duffel.booking

import com.travel.common.model.BookingRequest
import com.travel.common.model.BookingResult

interface DuffelBookingService {
    fun createBooking(request: BookingRequest): BookingResult
}
