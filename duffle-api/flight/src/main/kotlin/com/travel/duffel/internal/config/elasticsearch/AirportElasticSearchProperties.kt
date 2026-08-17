package com.travel.duffel.internal.config.elasticsearch

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "airports.search")
data class AirportElasticSearchProperties(
    val elasticsearchEnabled: Boolean = true,
)