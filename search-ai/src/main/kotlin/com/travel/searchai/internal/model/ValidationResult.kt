package com.travel.searchai.internal.model

// Field names mirror FlightIntent exactly (origin/destination/relativeDate/
// passengerCount/tripType/reply) so ValidationAgent composes with the existing
// extraction step without a translation layer. "valid" and "missingFields" are
// the only fields FlightIntent doesn't already have.
data class ValidationResult(
    val valid: Boolean,
    val missingFields: List<String> = emptyList(),
    val origin: String? = null,
    val destination: String? = null,
    val relativeDate: String? = null,
    val passengerCount: Int = 1,
    val tripType: String = "ONE_WAY",
    val reply: String = "",
)