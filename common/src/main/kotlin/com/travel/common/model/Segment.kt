package com.travel.common.model

import java.time.LocalDateTime

// Duffel reports segment times as airport-local wall-clock time with no UTC
// offset, so these are LocalDateTime rather than OffsetDateTime/Instant.
data class Segment(
    val flightNumber: String,
    val airline: Airline,
    val origin: Airport,
    val destination: Airport,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val durationMinutes: Long,
    val aircraftType: String? = null,
)
