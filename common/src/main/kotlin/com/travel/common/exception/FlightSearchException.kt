package com.travel.common.exception

open class FlightSearchException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
