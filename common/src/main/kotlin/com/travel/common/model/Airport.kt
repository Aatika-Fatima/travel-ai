package com.travel.common.model

data class Airport(
    val iataCode: String,
    val name: String,
    val city: String? = null,
    val countryCode: String? = null,
)
