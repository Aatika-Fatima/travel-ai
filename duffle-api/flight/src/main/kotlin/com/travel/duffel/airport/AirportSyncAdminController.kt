package com.travel.duffel.airport

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/airports")
class AirportSyncAdminController(
    private val syncService: DuffelAirportSyncService,
) {
    @PostMapping("/sync")
    fun triggerSync(
        @RequestParam(required = false) iataCountryCode: String?,
    ): AirportSyncSummary = syncService.syncAllAirports(iataCountryCode)
}
