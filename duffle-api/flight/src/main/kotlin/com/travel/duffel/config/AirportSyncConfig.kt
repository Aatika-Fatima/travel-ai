package com.travel.duffel.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(DuffelAirportSyncProperties::class)
class AirportSyncConfig
