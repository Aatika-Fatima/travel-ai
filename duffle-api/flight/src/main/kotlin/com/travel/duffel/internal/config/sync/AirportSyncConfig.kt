package com.travel.duffel.internal.config.sync

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(DuffelAirportSyncProperties::class)
class AirportSyncConfig
