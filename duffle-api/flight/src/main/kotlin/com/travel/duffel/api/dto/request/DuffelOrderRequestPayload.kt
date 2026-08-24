package com.travel.duffel.api.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

data class DuffelOrderRequestEnvelope(
    val data: DuffelOrderRequestPayload,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DuffelOrderRequestPayload(
    @JsonProperty("selected_offers")
    val selectedOffers: List<String>,
    val passengers: List<DuffelOrderPassengerRequest>,
    val payments: List<DuffelPaymentRequest>,
    val type: String = "instant",
    // order-service (§P8) stamps internal_order_id here on order creation --
    // it's the only key guaranteed to already exist on both sides before
    // Duffel's own order id is known, so it's what the order.created webhook
    // and the reconciliation sweep both correlate back on.
    val metadata: Map<String, String>? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DuffelOrderPassengerRequest(
    val id: String,
    val title: String,
    @JsonProperty("given_name")
    val givenName: String,
    @JsonProperty("family_name")
    val familyName: String,
    @JsonProperty("born_on")
    val bornOn: String,
    val gender: String,
    val email: String,
    @JsonProperty("phone_number")
    val phoneNumber: String,
    @JsonProperty("identity_documents")
    val identityDocuments: List<DuffelIdentityDocumentRequest>? = null,
)

data class DuffelIdentityDocumentRequest(
    val type: String,
    @JsonProperty("unique_identifier")
    val uniqueIdentifier: String,
    @JsonProperty("expires_on")
    val expiresOn: String,
    @JsonProperty("issuing_country_code")
    val issuingCountryCode: String,
)

data class DuffelPaymentRequest(
    val type: String,
    val currency: String,
    val amount: String,
)
