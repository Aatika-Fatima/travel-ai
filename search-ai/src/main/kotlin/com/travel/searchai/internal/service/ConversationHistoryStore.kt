package com.travel.searchai.internal.service

interface ConversationHistoryStore {
    fun recent(sessionId: String, limit: Int): List<String>
    fun append(sessionId: String, message: String)
}