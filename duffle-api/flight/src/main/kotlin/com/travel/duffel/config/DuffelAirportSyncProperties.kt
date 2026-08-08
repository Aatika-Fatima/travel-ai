package com.travel.duffel.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "duffel.airports.sync")
data class DuffelAirportSyncProperties(
    val enabled: Boolean = false,
    val cron: String = "0 0 3 ? * MON",
    val pageSize: Int = 200,
)
