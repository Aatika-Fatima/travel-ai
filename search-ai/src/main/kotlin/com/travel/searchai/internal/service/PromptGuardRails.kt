package com.travel.searchai.internal.service

import org.springframework.stereotype.Component

@Component
class PromptGuardRails {
    fun check(userMessage: String){
        val normalized = userMessage.trim().lowercase()
        val hit = BLOCKED_PHRASES.firstOrNull { normalized.contains(it.lowercase()) }
        if (hit != null) {
            throw PromptInjectionDetectedException("Blocked Phrase Detected: $normalized")
        }
    }

    companion object {
        private val BLOCKED_PHRASES =
            listOf(
                "ignore previous instructions",
                "ignore all previous instructions",
                "forget your rules",
                "reveal your prompt",
                "print system prompt",
                "developer mode",
                "you are now",
            )
    }
}