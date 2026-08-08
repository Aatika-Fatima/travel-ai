package com.travel.common.model

data class BookingResult(
    val orderId: String,
    val bookingReference: String,
    val status: String,
    val ticketingStatus: String? = null,
    val totalAmount: Money,
    val eTicketNumbers: List<String> = emptyList(),
    val passengers: List<BookedPassenger> = emptyList(),
)

data class BookedPassenger(
    val id: String,
    val givenName: String,
    val familyName: String,
)
