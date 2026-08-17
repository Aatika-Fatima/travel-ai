package com.travel.duffel.internal.config.cache.redis

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext

@Configuration
@EnableCaching
@EnableConfigurationProperties(AirportRedisCacheProperties::class)
class AirportRedisCacheConfig {

    @Bean
    fun airportCacheCustomizer(properties: AirportRedisCacheProperties): RedisCacheManagerBuilderCustomizer =
        RedisCacheManagerBuilderCustomizer { builder ->
            // Uses its own type-aware Jackson mapper (via .enableUnsafeDefaultTyping()) rather than the
            // app's shared REST JsonMapper: without embedded type info, Spring Cache can't reconstruct
            // List<AirportSummary> on a cache hit and returns raw LinkedHashMaps instead, which then fail
            // to cast. "Unsafe" is fine here since Redis is only ever populated by this app's own writes.
            val serializer =
                GenericJacksonJsonRedisSerializer
                    .builder()
                    .enableUnsafeDefaultTyping()
                    .build()
            val config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.ttl)
                .prefixCacheNameWith(properties.keyPrefix)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))

            builder.withCacheConfiguration("airportSearch", config)
        }
}