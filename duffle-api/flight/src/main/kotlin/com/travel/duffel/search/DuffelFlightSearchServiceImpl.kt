package com.travel.duffel.search

import com.travel.common.exception.ValidationException
import com.travel.common.model.FlightSearchRequest
import com.travel.common.model.FlightSearchResult
import com.travel.duffel.client.DuffelFlightClient
import org.springframework.stereotype.Service

@Service
class DuffelFlightSearchServiceImpl(
    private val duffelFlightClient: DuffelFlightClient,
) : DuffelFlightSearchService {
    override fun search(request: FlightSearchRequest): FlightSearchResult {
        if (request.passengers.total < 1) {
            throw ValidationException(
                "At least one passenger is required",
                errors = listOf("passengers: must include at least one adult"),
            )
        }

        val payload = DuffelRequestMapper.toPayload(request)
        val offers = duffelFlightClient.createOfferRequest(payload)
        return DuffelResponseMapper.toFlightSearchResult(
            searchId = null,
            offers = offers,
            requestedCabinClass = request.cabinClass,
        )
    }
}
