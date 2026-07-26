package com.travel.travelai.controller

import com.travel.travelai.service.TravelKnowledgeService
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.document.Document
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController

class ChatController(
    @Qualifier("geminiChatClient") val geminiChatClient: ChatClient,
    val service: TravelKnowledgeService
) {

    @GetMapping("/chat")
    fun ask(
        @RequestParam("query") query: String,
        @RequestParam("provider", defaultValue = "gemini") provider: String
    ): String? {

        val documents: List<Document> = service.similaritySearch(query)

        val context: String = documents.joinToString(separator = " \n")

        val systemPrompt = """
            You are a travel assistant.
            Answer only using this information:

            $context
        """.trimIndent()


        return geminiChatClient.prompt()
            .system(systemPrompt)
            .user(query)
            .call()
            .content()
    }
}