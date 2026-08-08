package com.travel.duffel.search

import com.travel.common.enums.CabinClass
import com.travel.common.enums.TripType
import com.travel.common.model.FlightSearchRequest
import com.travel.common.util.DateTimeUtils
import com.travel.duffel.dto.request.DuffelOfferRequestPayload
import com.travel.duffel.dto.request.DuffelPassengerRequest
import com.travel.duffel.dto.request.DuffelSliceRequest

object DuffelRequestMapper {
    fun toPayload(request: FlightSearchRequest): DuffelOfferRequestPayload {
        val slices =
            when (request.tripType) {
                TripType.MULTI_CITY ->
                    request.additionalSlices.map {
                        DuffelSliceRequest(
                            origin = it.origin.uppercase(),
                            destination = it.destination.uppercase(),
                            departureDate = DateTimeUtils.formatDate(it.departureDate),
                        )
                    }
                TripType.ROUND_TRIP ->
                    listOfNotNull(
                        DuffelSliceRequest(
                            origin = request.origin.uppercase(),
                            destination = request.destination.uppercase(),
                            departureDate = DateTimeUtils.formatDate(request.departureDate),
                        ),
                        request.returnDate?.let {
                            DuffelSliceRequest(
                                origin = request.destination.uppercase(),
                                destination = request.origin.uppercase(),
                                departureDate = DateTimeUtils.formatDate(it),
                            )
                        },
                    )
                TripType.ONE_WAY ->
                    listOf(
                        DuffelSliceRequest(
                            origin = request.origin.uppercase(),
                            destination = request.destination.uppercase(),
                            departureDate = DateTimeUtils.formatDate(request.departureDate),
                        ),
                    )
            }

        val passengers =
            buildList {
                repeat(request.passengers.adults) { add(DuffelPassengerRequest(type = "adult")) }
                repeat(request.passengers.children) { add(DuffelPassengerRequest(type = "child")) }
                repeat(request.passengers.infants) { add(DuffelPassengerRequest(type = "infant_without_seat")) }
            }

        return DuffelOfferRequestPayload(
            slices = slices,
            passengers = passengers,
            cabinClass = toDuffelCabinClass(request.cabinClass),
            maxConnections = request.maxConnections,
            currency = request.currency,
        )
    }

    private fun toDuffelCabinClass(cabinClass: CabinClass): String =
        when (cabinClass) {
            CabinClass.ECONOMY -> "economy"
            CabinClass.PREMIUM_ECONOMY -> "premium_economy"
            CabinClass.BUSINESS -> "business"
            CabinClass.FIRST -> "first"
        }
}
