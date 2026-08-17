package com.travel.searchai.internal.config.cache.redis

import com.travel.searchai.internal.service.ConversationHistoryStore
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class RedisConversationHistoryStore(
    private val cacheManager: CacheManager,
) : ConversationHistoryStore {
    // Cache.get(key, Class<T>) can't express a parameterized type — generics are erased at
    // runtime, so List::class.java is the only token available. The cast is safe because the
    // Redis serializer stores type info alongside the value (unsafe default typing, same as
    // AirportRedisCacheConfig), so Jackson already reconstructs a List<String> on read.
    @Suppress("UNCHECKED_CAST")
    override fun recent(sessionId: String, limit: Int): List<String> {
        val history = cacheManager.getCache(CACHE_NAME)?.get(sessionId, List::class.java) as? List<String>
        return (history ?: emptyList()).takeLast(limit)
    }

    override fun append(sessionId: String, message: String) {
        val cache = cacheManager.getCache(CACHE_NAME) ?: return
        val existing = recent(sessionId, limit = MAX_HISTORY)
        cache.put(sessionId, (existing + message).takeLast(MAX_HISTORY))
    }

    companion object {
        const val CACHE_NAME = "assistantHistory"
        const val MAX_HISTORY = 20
    }
}