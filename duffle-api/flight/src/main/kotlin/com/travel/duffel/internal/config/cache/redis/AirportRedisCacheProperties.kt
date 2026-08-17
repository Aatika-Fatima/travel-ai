package com.travel.duffel.internal.config.cache.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "airports.cache")
data class AirportRedisCacheProperties (
    val ttl: Duration = Duration.ofHours(24),
    val keyPrefix: String = "airport-search::"
)