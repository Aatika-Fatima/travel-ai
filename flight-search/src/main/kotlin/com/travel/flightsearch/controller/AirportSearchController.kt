package com.travel.flightsearch.controller

import com.travel.common.model.AirportSummary
import com.travel.duffel.airport.AirportSearchService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/airports")
class AirportSearchController(
    private val airportSearchService: AirportSearchService,
) {
    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
    ): List<AirportSummary> = airportSearchService.search(q)
}
