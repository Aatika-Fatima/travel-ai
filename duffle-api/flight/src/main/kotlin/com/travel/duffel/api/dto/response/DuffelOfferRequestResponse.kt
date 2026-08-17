package com.travel.duffel.api.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelOfferRequestResponse(
    val data: DuffelOfferRequestData,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelOfferRequestData(
    val id: String,
    val offers: List<DuffelOffer> = emptyList(),
)