package com.travel.common.exception

class ValidationException(
    message: String,
    val errors: List<String> = emptyList(),
) : FlightSearchException(message)
