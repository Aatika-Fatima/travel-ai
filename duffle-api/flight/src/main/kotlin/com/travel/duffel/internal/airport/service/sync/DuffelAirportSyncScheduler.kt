package com.travel.duffel.internal.airport.service.sync

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "duffel.airports.sync", name = ["enabled"], havingValue = "true")
class DuffelAirportSyncScheduler(
    private val syncService: DuffelAirportSyncService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${duffel.airports.sync.cron:0 0 3 ? * MON}")
    fun scheduledSync() {
        runCatching { syncService.syncAllAirports() }
            .onFailure { log.error("Scheduled Duffel airport sync failed", it) }
    }
}
