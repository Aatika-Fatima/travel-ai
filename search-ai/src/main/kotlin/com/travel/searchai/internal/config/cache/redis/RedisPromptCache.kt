package com.travel.searchai.internal.config.cache.redis

import com.travel.searchai.internal.service.PromptCache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class RedisPromptCache(
    private val cacheManager: CacheManager,
) : PromptCache {
    override fun get(key: String): String? = cacheManager.getCache(CACHE_NAME)?.get(key, String::class.java)

    override fun put(key: String, value: String) {
        cacheManager.getCache(CACHE_NAME)?.put(key, value)
    }

    companion object {
        const val CACHE_NAME = "promptRender"
    }
}
