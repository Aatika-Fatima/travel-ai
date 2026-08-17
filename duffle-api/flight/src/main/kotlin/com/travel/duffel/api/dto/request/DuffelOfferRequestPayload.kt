package com.travel.duffel.api.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

data class DuffelOfferRequestEnvelope(
    val data: DuffelOfferRequestPayload,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DuffelOfferRequestPayload(
    val slices: List<DuffelSliceRequest>,
    val passengers: List<DuffelPassengerRequest>,
    @JsonProperty("cabin_class")
    val cabinClass: String? = null,
    @JsonProperty("max_connections")
    val maxConnections: Int? = null,
    // Duffel takes currency conversion as a query parameter on the request URL,
    // not a body field - carried here only so DuffelFlightClient can read it.
    @JsonIgnore
    val currency: String? = null,
)

data class DuffelSliceRequest(
    val origin: String,
    val destination: String,
    @JsonProperty("departure_date")
    val departureDate: String,
)

data class DuffelPassengerRequest(
    val type: String = "adult",
)