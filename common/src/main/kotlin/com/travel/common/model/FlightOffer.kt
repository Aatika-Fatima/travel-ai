package com.travel.common.model

import com.travel.common.enums.CabinClass

data class FlightOffer(
    val id: String,
    val price: Money,
    val airlines: List<Airline>,
    val slices: List<Slice>,
    val cabinClass: CabinClass,
    val totalDurationMinutes: Long,
    val baggageAllowance: String? = null,
    val refundable: Boolean = false,
    val seatsRemaining: Int? = null,
    val expiresAt: String? = null,
) {
    val stops: Int get() = slices.sumOf { it.stops }
}

data class FlightSearchResult(
    val searchId: String? = null,
    val offers: List<FlightOffer>,
) {
    val totalResults: Int get() = offers.size
}
