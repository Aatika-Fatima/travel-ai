package com.travel.searchai.internal.service.validation

import com.travel.searchai.internal.model.FlightIntent
import com.travel.searchai.internal.model.ValidationResult
import com.travel.searchai.internal.service.PromptOrchestrator
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class ValidationAgent(@Qualifier("geminiChatClient")private val chatClient: ChatClient,
                      private val promptOrchestrator: PromptOrchestrator,
                      private val objectMapper: ObjectMapper
) {

    fun validate(intent: FlightIntent, sessionId: String?=null): ValidationResult {
        val intentJson = objectMapper.writeValueAsString(intent);
        val prompt = promptOrchestrator
            .build("flight-validation-agent",intentJson,sessionId)
        return chatClient.prompt(prompt).call().entity(ValidationResult::class.java)
            ?:error("Empty Response From Gemini Client")
    }

    private val RELATIVE_DATE = Regex("""^(today|tomorrow|unknown|in_\d+_days)$""")

    fun validateDeterministic(intent: FlightIntent): ValidationResult {
        val missing = buildList {
            if (intent.origin.isNullOrBlank()) add("origin")
            if (intent.destination.isNullOrBlank()) add("destination")
            val date = intent.relativeDate
            if (date.isNullOrBlank() || date == "unknown" || !RELATIVE_DATE.matches(date)) add("relativeDate")
        }
        val sameCity = intent.origin != null &&
                intent.origin.equals(intent.destination, ignoreCase = true)

        return ValidationResult(
            valid = missing.isEmpty() && !sameCity && intent.passengerCount > 0,
            missingFields = missing,
            origin = intent.origin,
            destination = intent.destination,
            relativeDate = intent.relativeDate,
            passengerCount = intent.passengerCount,
            tripType = intent.tripType,
            reply = when {
                sameCity -> "Origin and destination cannot be the same."
                missing.isNotEmpty() -> "" // let the supervisor's existing missing-field copy handle this
                else -> ""
            },
        )
    }


}