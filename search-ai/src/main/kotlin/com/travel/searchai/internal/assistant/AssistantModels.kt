package com.travel.searchai.internal.assistant

data class AssistantMessageRequest(
    val message: String,
    val sessionID:String? = null,
)

data class AssistantAction(
    val type: String,
    val origin: String,
    val destination: String,
    val departureDate: String,
    val tripType: String,
)

data class AssistantMessageResponse(
    val reply: String,
    val action: AssistantAction? = null,
)

// Shape Gemini is prompted to return - kept separate from AssistantMessageResponse since the
// model deals in free-text city/airport names and a coarse relative date, not resolved IATA
// codes or ISO dates.
internal data class MistralFlightIntent(
    val origin: String? = null,
    val destination: String? = null,
    val relativeDate: String? = null,
    val tripType: String? = null,
    val reply: String = "",
)
