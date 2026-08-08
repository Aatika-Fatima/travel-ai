package com.travel.duffel.search

import com.travel.common.enums.CabinClass
import com.travel.common.model.Airline
import com.travel.common.model.Airport
import com.travel.common.model.FlightOffer
import com.travel.common.model.FlightSearchResult
import com.travel.common.model.Money
import com.travel.common.model.Segment
import com.travel.common.model.Slice
import com.travel.duffel.dto.response.DuffelCarrier
import com.travel.duffel.dto.response.DuffelOffer
import com.travel.duffel.dto.response.DuffelPlace
import com.travel.duffel.dto.response.DuffelSegment
import com.travel.duffel.dto.response.DuffelSlice
import java.time.Duration
import java.time.LocalDateTime

object DuffelResponseMapper {
    fun toFlightSearchResult(
        searchId: String?,
        offers: List<DuffelOffer>,
        requestedCabinClass: CabinClass,
    ): FlightSearchResult = FlightSearchResult(searchId = searchId, offers = offers.map { toFlightOffer(it, requestedCabinClass) })

    fun toFlightOffer(
        offer: DuffelOffer,
        requestedCabinClass: CabinClass,
    ): FlightOffer {
        val slices = offer.slices.map { toSlice(it) }
        return FlightOffer(
            id = offer.id,
            price = Money(amount = offer.totalAmount.toDoubleOrNull() ?: 0.0, currency = offer.totalCurrency),
            airlines =
                offer.slices
                    .flatMap { it.segments }
                    .map { it.marketingCarrier }
                    .distinctBy { it.iataCode ?: it.name }
                    .map { toAirline(it) },
            slices = slices,
            cabinClass = resolveCabinClass(offer) ?: requestedCabinClass,
            totalDurationMinutes = slices.sumOf { it.durationMinutes },
            baggageAllowance = summarizeBaggage(offer),
            refundable = offer.conditions?.refundBeforeDeparture?.allowed ?: false,
            seatsRemaining = null,
            expiresAt = offer.expiresAt,
        )
    }

    private fun toSlice(slice: DuffelSlice): Slice {
        val segments = slice.segments.map { toSegment(it) }
        return Slice(
            origin = toAirport(slice.origin),
            destination = toAirport(slice.destination),
            departureTime = segments.first().departureTime,
            arrivalTime = segments.last().arrivalTime,
            durationMinutes =
                parseDurationMinutes(slice.duration)
                    ?: Duration.between(segments.first().departureTime, segments.last().arrivalTime).toMinutes(),
            segments = segments,
        )
    }

    private fun toSegment(segment: DuffelSegment): Segment =
        Segment(
            flightNumber = "${segment.marketingCarrier.iataCode.orEmpty()}${segment.marketingCarrierFlightNumber.orEmpty()}",
            airline = toAirline(segment.marketingCarrier),
            origin = toAirport(segment.origin),
            destination = toAirport(segment.destination),
            departureTime = LocalDateTime.parse(segment.departingAt),
            arrivalTime = LocalDateTime.parse(segment.arrivingAt),
            durationMinutes = parseDurationMinutes(segment.duration) ?: 0,
            aircraftType = segment.aircraft?.name,
        )

    private fun toAirport(place: DuffelPlace): Airport =
        Airport(
            iataCode = place.iataCode.orEmpty(),
            name = place.name.orEmpty(),
            city = place.cityName,
            countryCode = place.iataCountryCode,
        )

    private fun toAirline(carrier: DuffelCarrier): Airline = Airline(name = carrier.name, iataCode = carrier.iataCode, logoUrl = carrier.logoSymbolUrl)

    private fun resolveCabinClass(offer: DuffelOffer): CabinClass? {
        val raw = offer.slices.firstOrNull()?.segments?.firstOrNull()?.passengers?.firstOrNull()?.cabinClass ?: return null
        return when (raw.lowercase()) {
            "economy" -> CabinClass.ECONOMY
            "premium_economy" -> CabinClass.PREMIUM_ECONOMY
            "business" -> CabinClass.BUSINESS
            "first" -> CabinClass.FIRST
            else -> null
        }
    }

    private fun summarizeBaggage(offer: DuffelOffer): String? {
        val baggage =
            offer.slices
                .firstOrNull()
                ?.segments
                ?.firstOrNull()
                ?.passengers
                ?.firstOrNull()
                ?.baggages
                ?.firstOrNull() ?: return null
        val quantity = baggage.quantity ?: return null
        val type = baggage.type ?: "baggage"
        return "$quantity x $type"
    }

    private fun parseDurationMinutes(iso: String?): Long? = iso?.let { runCatching { Duration.parse(it).toMinutes() }.getOrNull() }
}
