package com.travel.duffel.api.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelOfferResponse(
    val data: DuffelOffer,
)
