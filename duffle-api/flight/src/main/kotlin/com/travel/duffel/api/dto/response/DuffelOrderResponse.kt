package com.travel.duffel.api.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelOrderResponse(
    val data: DuffelOrderData,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelOrderData(
    val id: String,
    @JsonProperty("booking_reference")
    val bookingReference: String? = null,
    val status: String? = null,
    @JsonProperty("total_amount")
    val totalAmount: String? = null,
    @JsonProperty("total_currency")
    val totalCurrency: String? = null,
    val documents: List<DuffelOrderDocument>? = null,
    val passengers: List<DuffelOfferPassenger>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelOrderDocument(
    val type: String? = null,
    @JsonProperty("unique_identifier")
    val uniqueIdentifier: String? = null,
)
