package com.travel.duffel.internal.config.client

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "duffel")
data class DuffelProperties(
    val apiKey: String,
    val baseUrl: String = "https://api.duffel.com",
    val version: String = "v2",
    val timeoutMs: Long = 10_000L,
)