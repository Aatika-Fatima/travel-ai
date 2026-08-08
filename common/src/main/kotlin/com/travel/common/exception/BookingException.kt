package com.travel.common.exception

enum class BookingFailureReason {
    OFFER_EXPIRED,
    VALIDATION_FAILED,
    PAYMENT_DECLINED,
    UNKNOWN,
}

class BookingException(
    message: String,
    val reason: BookingFailureReason = BookingFailureReason.UNKNOWN,
    val fieldErrors: Map<String, String> = emptyMap(),
    cause: Throwable? = null,
) : FlightSearchException(message, cause)
