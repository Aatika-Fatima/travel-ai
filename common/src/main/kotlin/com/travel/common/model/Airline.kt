package com.travel.common.model

data class Airline(
    val name: String,
    val iataCode: String? = null,
    val logoUrl: String? = null,
)
