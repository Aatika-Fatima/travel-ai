package com.travel.duffel.api.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelCarrier(
    val id: String? = null,
    val name: String,
    @JsonProperty("iata_code")
    val iataCode: String? = null,
    @JsonProperty("logo_symbol_url")
    val logoSymbolUrl: String? = null,
    @JsonProperty("logo_lockup_url")
    val logoLockupUrl: String? = null,
    @JsonProperty("conditions_of_carriage_url")
    val conditionsOfCarriageUrl: String? = null,
)