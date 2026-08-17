package com.travel.duffel.internal.airport.controller

import com.travel.duffel.internal.airport.service.AirportReindexService
import com.travel.duffel.internal.airport.service.sync.AirportSyncSummary
import com.travel.duffel.internal.airport.service.sync.DuffelAirportSyncService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/airports")
class AirportSyncAdminController(
    private val syncService: DuffelAirportSyncService,
    private val reindexService: AirportReindexService,
) {
    @PostMapping("/sync")
    fun triggerSync(
        @RequestParam(required = false) iataCountryCode: String?,
    ): AirportSyncSummary = syncService.syncAllAirports(iataCountryCode)

    // new
    @PostMapping("/reindex")
    fun triggerReindex():Map<String,Int> = mapOf("reindexed" to reindexService.reindexAll())
}