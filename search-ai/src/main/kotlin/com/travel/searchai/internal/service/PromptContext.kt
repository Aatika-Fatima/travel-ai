package com.travel.searchai.internal.service

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class PromptContext(
    val sessionId: String?,
    val recentMessages: List<String>?,
    val currentDate: OffsetDateTime,
    val userMessage: String,
) {
    fun getContext(): Map<String, Any> {
        return mapOf(
            "currentDate" to currentDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            "timeZone" to currentDate.offset.id,
            "locale" to Locale.getDefault().toLanguageTag(),
            "userMessage" to userMessage,
        )
    }
}