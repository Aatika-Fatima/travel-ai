package com.travel.searchai.internal.service

import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class ContextBuilder(private val conversationHistoryStore: ConversationHistoryStore) {

    fun build(sessionId: String?, userMessage: String): PromptContext {
        val recent = sessionId?.let { conversationHistoryStore.recent(it, limit = 5) } ?: emptyList()
        return PromptContext(
            sessionId = sessionId,
            recentMessages = recent,
            currentDate = OffsetDateTime.now(),
            userMessage = userMessage,
        )
    }
}