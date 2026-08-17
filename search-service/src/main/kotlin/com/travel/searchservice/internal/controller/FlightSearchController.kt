package com.travel.searchservice.internal.controller

import com.travel.common.model.BookingRequest
import com.travel.common.model.BookingResult
import com.travel.common.model.FlightSearchRequest
import com.travel.common.model.FlightSearchResult
import com.travel.duffel.api.booking.DuffelBookingService
import com.travel.duffel.api.search.DuffelFlightSearchService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/flights")
class FlightSearchController(
    private val flightSearchService: DuffelFlightSearchService,
    private val bookingService: DuffelBookingService,
) {
    @PostMapping("/search")
    fun search(
        @Valid @RequestBody request: FlightSearchRequest,
    ): FlightSearchResult = flightSearchService.search(request)

    @PostMapping("/book")
    fun book(
        @Valid @RequestBody request: BookingRequest,
    ): BookingResult = bookingService.createBooking(request)
}
