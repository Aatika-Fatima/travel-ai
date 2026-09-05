package com.travel.searchai.internal.service.intent

import com.travel.searchai.internal.model.FlightIntent
import com.travel.searchai.internal.service.PromptOrchestrator
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class IntentExtractionAgent(
    @Qualifier("geminiChatClient") private val chatClient: ChatClient,
    private val promptOrchestrator: PromptOrchestrator,
) {
    fun extract(message: String, sessionId: String? = null): FlightIntent {
        val prompt = promptOrchestrator.build(FLIGHT_SEARCH_PROMPT, message, sessionId)
        return chatClient.prompt(prompt).call().entity(FlightIntent::class.java)
            ?: error("Empty response from Gemini")
    }

    companion object {
        private const val FLIGHT_SEARCH_PROMPT = "flight-search"
    }
}