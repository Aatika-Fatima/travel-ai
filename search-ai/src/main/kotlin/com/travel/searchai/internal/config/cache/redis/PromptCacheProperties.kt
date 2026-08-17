package com.travel.searchai.internal.config.cache.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "prompts.cache")
class PromptCacheProperties(
    val ttl: Duration = Duration.ofMinutes(10),
    val keyPrefix:String = "prompt-render::",
    val historyTtl:Duration = Duration.ofHours(2),
    val historyKeyPrefix:String="assistant-history::")