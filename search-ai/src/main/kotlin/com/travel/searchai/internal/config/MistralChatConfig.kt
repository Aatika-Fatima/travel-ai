package com.travel.searchai.internal.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.mistralai.MistralAiChatModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MistralChatConfig {
    @Bean
    fun mistralChatClient(chatModel: MistralAiChatModel): ChatClient = ChatClient.builder(chatModel).build()
}
