package com.travel.common.model

data class AirportSummary(
    val iataCode: String,
    val name: String,
    val cityName: String?,
    val iataCountryCode: String?,
    val source: String,
)
