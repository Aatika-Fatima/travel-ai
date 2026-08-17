package com.travel.searchai.internal.assistant

import com.travel.common.model.AirportSummary
import com.travel.duffel.api.airport.AirportSearchService
import com.travel.searchai.internal.model.FlightIntent
import com.travel.searchai.internal.service.PromptInjectionDetectedException
import com.travel.searchai.internal.service.PromptOrchestrator
import com.travel.searchai.internal.service.validation.ValidationAgent
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.LocalDate

// Mistral extracts free-text intent (city/airport names, a coarse relative date); airport name
// -> IATA code resolution stays with the existing AirportSearchService rather than trusting the
// model to know real airport codes.
@Service
class FlightSearchAssistantService(
    @Qualifier("mistralChatClient") private val chatClient: ChatClient,
    private val airportSearchService: AirportSearchService,
    private val promptOrchestrator: PromptOrchestrator,
    private val validationAgent: ValidationAgent,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(message: String, sessionId: String? = null): AssistantMessageResponse {
        val text = message.trim()
        if (text.isEmpty()) {
            return AssistantMessageResponse(
                "Tell me where you'd like to fly - e.g. \"flights from Delhi to Mumbai tomorrow\".",
            )
        }

        val intent =
            runCatching { extractIntent(text, sessionId) }
                .getOrElse {
                    log.warn("Prompt pipeline rejected or Mistral failed: {}", it.message)
                    return AssistantMessageResponse(
                        if (it is PromptInjectionDetectedException) {
                            "I can only help with flight search and booking."
                        } else {
                            "Sorry, I couldn't process that right now. Please try again."
                        },
                    )
                }

        // Deterministic checks only - these are plain boolean logic over already-extracted
        // fields (mandatory fields, same-city, date format), so no second Mistral round-trip.
        val validation = validationAgent.validateDeterministic(intent)
        if (!validation.valid) {
            return AssistantMessageResponse(validation.reply.ifBlank { intent.reply.ifBlank { ASK_WHERE_TO_FLY } })
        }

        // normalize() also catches the model literally emitting the string "null", which
        // validateDeterministic()'s isNullOrBlank() check alone wouldn't reject.
        val originText = normalize(validation.origin)
        val destinationText = normalize(validation.destination)
        if (originText == null || destinationText == null) {
            return AssistantMessageResponse(intent.reply.ifBlank { ASK_WHERE_TO_FLY })
        }

        val origin = resolveAirport(originText)
        val destination = resolveAirport(destinationText)
        if (origin == null || destination == null) {
            val unresolved = listOfNotNull(originText.takeIf { origin == null }, destinationText.takeIf { destination == null })
            return AssistantMessageResponse(
                "I couldn't find an airport matching \"${unresolved.joinToString(", ")}\". Try a city name or IATA code.",
            )
        }

        return AssistantMessageResponse(
            reply = intent.reply.ifBlank { "Here are flights from ${label(origin)} to ${label(destination)}." },
            action =
                AssistantAction(
                    type = "SEARCH_FLIGHTS",
                    origin = origin.iataCode,
                    destination = destination.iataCode,
                    departureDate = resolveDate(validation.relativeDate).toString(),
                    tripType = if (validation.tripType.uppercase() == "ROUND_TRIP") "ROUND_TRIP" else "ONE_WAY",
                ),
        )
    }

    private fun extractIntent(message: String, sessionId: String?): FlightIntent {
        val prompt = promptOrchestrator.build(FLIGHT_SEARCH_PROMPT, message, sessionId)
        return chatClient.prompt(prompt).call().entity(FlightIntent::class.java)
            ?: error("Empty response from Mistral")
    }

    private fun normalize(value: String?): String? = value?.trim()?.takeUnless { it.isEmpty() || it.equals("null", ignoreCase = true) }

    private fun resolveAirport(term: String): AirportSummary? = runCatching { airportSearchService.search(term) }.getOrNull()?.firstOrNull()

    private fun resolveDate(relativeDate: String?): LocalDate {
        val normalized = normalize(relativeDate)?.lowercase()
        return when {
            normalized == null || normalized == "unknown" -> LocalDate.now().plusDays(DEFAULT_DATE_OFFSET_DAYS)
            normalized == "today" -> LocalDate.now()
            normalized == "tomorrow" -> LocalDate.now().plusDays(1)
            else ->
                IN_DAYS_PATTERN
                    .find(normalized)
                    ?.let { LocalDate.now().plusDays(it.groupValues[1].toLong()) }
                    ?: LocalDate.now().plusDays(DEFAULT_DATE_OFFSET_DAYS)
        }
    }

    private fun label(airport: AirportSummary): String = "${airport.cityName ?: airport.name} (${airport.iataCode})"

    companion object {
        private const val FLIGHT_SEARCH_PROMPT = "flight-search"
        private const val ASK_WHERE_TO_FLY = "Where would you like to fly from and to?"
        private const val DEFAULT_DATE_OFFSET_DAYS = 7L
        private val IN_DAYS_PATTERN = Regex("""in_(\d+)_days""")
    }
}