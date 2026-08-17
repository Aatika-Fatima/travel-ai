package com.travel.searchai.internal.model


// Mirrors the JSON contract in prompt/flight-search/system/v1.st exactly — if one changes,
// the other must too. Kept separate from AssistantMessageResponse: the model deals in free-text
// city/airport names and a coarse relative date, not resolved IATA codes or ISO dates.
data class FlightIntent(
    val origin: String? = null,
    val destination: String? = null,
    val relativeDate: String? = null,
    val passengerCount: Int = 1,
    val tripType: String = "ONE_WAY",
    val reply: String = "",
)