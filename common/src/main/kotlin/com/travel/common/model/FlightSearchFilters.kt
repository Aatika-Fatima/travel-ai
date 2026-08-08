package com.travel.common.model

data class FlightSearchFilters(
    val maxStops: Int? = null,
    val airlines: List<String> = emptyList(),
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val departureTimeWindow: TimeWindow? = null,
    val arrivalTimeWindow: TimeWindow? = null,
    val baggageIncluded: Boolean? = null,
)
