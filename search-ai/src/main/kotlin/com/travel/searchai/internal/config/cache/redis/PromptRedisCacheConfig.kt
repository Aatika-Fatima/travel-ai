package com.travel.searchai.internal.config.cache.redis

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration

@Configuration
@EnableConfigurationProperties(PromptCacheProperties::class)
class PromptRedisCacheConfig {

    @Bean
    fun promptCacheCustomizer(properties: PromptCacheProperties): RedisCacheManagerBuilderCustomizer =
        RedisCacheManagerBuilderCustomizer { builder ->
            val renderConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(properties.ttl)
                    .prefixCacheNameWith(properties.keyPrefix)
            val historyConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(properties.historyTtl)
                    .prefixCacheNameWith(properties.historyKeyPrefix)

            builder
                .withCacheConfiguration("promptRender", renderConfig)
                .withCacheConfiguration("assistantHistory", historyConfig)
        }
}