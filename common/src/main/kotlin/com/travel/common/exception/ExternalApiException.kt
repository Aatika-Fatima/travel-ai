package com.travel.common.exception

class ExternalApiException(
    message: String,
    val statusCode: Int? = null,
    val retryable: Boolean = false,
    cause: Throwable? = null,
) : FlightSearchException(message, cause)
