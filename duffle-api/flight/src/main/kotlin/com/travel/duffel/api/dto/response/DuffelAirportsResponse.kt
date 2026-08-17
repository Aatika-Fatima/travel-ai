package com.travel.duffel.api.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelAirportsResponse(
    val data: List<DuffelAirportRecord> = emptyList(),
    val meta: DuffelPageMeta = DuffelPageMeta(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelAirportRecord(
    val id: String,
    val name: String,
    @JsonProperty("iata_code")
    val iataCode: String? = null,
    @JsonProperty("icao_code")
    val icaoCode: String? = null,
    @JsonProperty("iata_city_code")
    val iataCityCode: String? = null,
    @JsonProperty("city_name")
    val cityName: String? = null,
    @JsonProperty("iata_country_code")
    val iataCountryCode: String? = null,
    val city: DuffelPlace? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @JsonProperty("time_zone")
    val timeZone: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelPageMeta(
    val before: String? = null,
    val after: String? = null,
    val limit: Int? = null,
)
