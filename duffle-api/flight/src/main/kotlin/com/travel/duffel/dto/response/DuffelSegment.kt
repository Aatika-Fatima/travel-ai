package com.travel.duffel.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelSegment(
    val id: String? = null,
    val origin: DuffelPlace,
    val destination: DuffelPlace,
    @JsonProperty("origin_terminal")
    val originTerminal: String? = null,
    @JsonProperty("destination_terminal")
    val destinationTerminal: String? = null,
    @JsonProperty("departing_at")
    val departingAt: String,
    @JsonProperty("arriving_at")
    val arrivingAt: String,
    val duration: String? = null,
    val distance: String? = null,
    val aircraft: DuffelAircraft? = null,
    @JsonProperty("marketing_carrier")
    val marketingCarrier: DuffelCarrier,
    @JsonProperty("operating_carrier")
    val operatingCarrier: DuffelCarrier? = null,
    @JsonProperty("marketing_carrier_flight_number")
    val marketingCarrierFlightNumber: String? = null,
    @JsonProperty("operating_carrier_flight_number")
    val operatingCarrierFlightNumber: String? = null,
    val stops: List<DuffelStop>? = null,
    val media: List<DuffelMedia>? = null,
    val passengers: List<DuffelSegmentPassenger>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelAircraft(
    val id: String? = null,
    val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelStop(
    val id: String? = null,
    val airport: DuffelPlace? = null,
    val duration: String? = null,
    @JsonProperty("arriving_at")
    val arrivingAt: String? = null,
    @JsonProperty("departing_at")
    val departingAt: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelMedia(
    val type: String? = null,
    val href: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelSegmentPassenger(
    @JsonProperty("passenger_id")
    val passengerId: String? = null,
    @JsonProperty("cabin_class")
    val cabinClass: String? = null,
    @JsonProperty("cabin_class_marketing_name")
    val cabinClassMarketingName: String? = null,
    @JsonProperty("fare_basis_code")
    val fareBasisCode: String? = null,
    val baggages: List<DuffelBaggage>? = null,
    val cabin: DuffelCabin? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelBaggage(
    val type: String? = null,
    val quantity: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelCabin(
    val name: String? = null,
    @JsonProperty("marketing_name")
    val marketingName: String? = null,
    val amenities: DuffelCabinAmenities? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelCabinAmenities(
    val seat: DuffelSeatAmenity? = null,
    val wifi: DuffelWifiAmenity? = null,
    val power: DuffelPowerAmenity? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelSeatAmenity(
    val type: String? = null,
    val pitch: String? = null,
    val legroom: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelWifiAmenity(
    val available: Boolean = false,
    val cost: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelPowerAmenity(
    val available: Boolean = false,
)