package com.travel.duffel.api.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelPlace(
    val id: String? = null,
    val type: String? = null,
    val name: String? = null,
    @JsonProperty("iata_code")
    val iataCode: String? = null,
    @JsonProperty("icao_code")
    val icaoCode: String? = null,
    @JsonProperty("iata_city_code")
    val iataCityCode: String? = null,
    @JsonProperty("iata_country_code")
    val iataCountryCode: String? = null,
    @JsonProperty("city_name")
    val cityName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @JsonProperty("time_zone")
    val timeZone: String? = null,
    val city: DuffelPlace? = null,
)