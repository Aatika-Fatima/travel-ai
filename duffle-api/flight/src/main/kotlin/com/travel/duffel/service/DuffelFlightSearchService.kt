package com.travel.duffel.service

import com.travel.duffel.client.DuffelFlightClient
import com.travel.duffel.dto.request.DuffelOfferRequestPayload
import com.travel.duffel.dto.request.DuffelPassengerRequest
import com.travel.duffel.dto.request.DuffelSliceRequest
import com.travel.duffel.dto.response.DuffelOffer
import org.springframework.stereotype.Service

@Service
class
DuffelFlightSearchService(
    private val duffelFlightClient: DuffelFlightClient,
) {
    fun searchFlights(
        origin: String,
        destination: String,
        departureDate: String,
        passengers: Int,
        cabinClass: String? = null,
    ): List<DuffelOffer> {
        require(passengers > 0) { "passengers must be at least 1" }

        val payload =
            DuffelOfferRequestPayload(
                slices =
                    listOf(
                        DuffelSliceRequest(
                            origin = origin,
                            destination = destination,
                            departureDate = departureDate,
                        ),
                    ),
                passengers = List(passengers) { DuffelPassengerRequest() },
                cabinClass = cabinClass,
            )

        return duffelFlightClient.createOfferRequest(payload)
    }
}