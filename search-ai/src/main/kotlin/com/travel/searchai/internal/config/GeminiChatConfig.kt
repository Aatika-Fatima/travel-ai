package com.travel.searchai.internal.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.google.genai.GoogleGenAiChatModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GeminiChatConfig {
    @Bean
    fun geminiChatClient(chatModel: GoogleGenAiChatModel): ChatClient = ChatClient.builder(chatModel).build()
}
