package com.travel.duffel.api.search

import com.travel.common.model.FlightSearchRequest
import com.travel.common.model.FlightSearchResult

interface DuffelFlightSearchService {
    fun search(request: FlightSearchRequest): FlightSearchResult
}
