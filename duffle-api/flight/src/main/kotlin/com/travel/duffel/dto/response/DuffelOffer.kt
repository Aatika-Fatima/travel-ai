package com.travel.duffel.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelOffer(
    val id: String,
    @JsonProperty("live_mode")
    val liveMode: Boolean = false,
    val partial: Boolean = false,
    @JsonProperty("created_at")
    val createdAt: String? = null,
    @JsonProperty("updated_at")
    val updatedAt: String? = null,
    @JsonProperty("expires_at")
    val expiresAt: String? = null,
    @JsonProperty("base_amount")
    val baseAmount: String? = null,
    @JsonProperty("base_currency")
    val baseCurrency: String? = null,
    @JsonProperty("tax_amount")
    val taxAmount: String? = null,
    @JsonProperty("tax_currency")
    val taxCurrency: String? = null,
    @JsonProperty("total_amount")
    val totalAmount: String,
    @JsonProperty("total_currency")
    val totalCurrency: String,
    @JsonProperty("intended_base_amount")
    val intendedBaseAmount: String? = null,
    @JsonProperty("intended_total_amount")
    val intendedTotalAmount: String? = null,
    @JsonProperty("total_emissions_kg")
    val totalEmissionsKg: String? = null,
    val owner: DuffelCarrier,
    val slices: List<DuffelSlice>,
    val passengers: List<DuffelOfferPassenger>? = null,
    val conditions: DuffelOfferConditions? = null,
    @JsonProperty("payment_requirements")
    val paymentRequirements: DuffelPaymentRequirements? = null,
    @JsonProperty("available_services")
    val availableServices: List<DuffelAvailableService>? = null,
    @JsonProperty("supported_loyalty_programmes")
    val supportedLoyaltyProgrammes: List<String>? = null,
    @JsonProperty("private_fares")
    val privateFares: List<DuffelPrivateFare>? = null,
    @JsonProperty("supported_passenger_identity_document_types")
    val supportedPassengerIdentityDocumentTypes: List<String>? = null,
    @JsonProperty("passenger_identity_documents_required")
    val passengerIdentityDocumentsRequired: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelOfferPassenger(
    val id: String? = null,
    val type: String? = null,
    val age: Int? = null,
    @JsonProperty("given_name")
    val givenName: String? = null,
    @JsonProperty("family_name")
    val familyName: String? = null,
    @JsonProperty("fare_type")
    val fareType: String? = null,
    @JsonProperty("loyalty_programme_accounts")
    val loyaltyProgrammeAccounts: List<DuffelLoyaltyProgrammeAccount>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelLoyaltyProgrammeAccount(
    @JsonProperty("airline_iata_code")
    val airlineIataCode: String? = null,
    @JsonProperty("account_number")
    val accountNumber: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelPaymentRequirements(
    @JsonProperty("requires_instant_payment")
    val requiresInstantPayment: Boolean = false,
    @JsonProperty("price_guarantee_expires_at")
    val priceGuaranteeExpiresAt: String? = null,
    @JsonProperty("payment_required_by")
    val paymentRequiredBy: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelAvailableService(
    val id: String? = null,
    val type: String? = null,
    @JsonProperty("total_amount")
    val totalAmount: String? = null,
    @JsonProperty("total_currency")
    val totalCurrency: String? = null,
    val metadata: Map<String, String>? = null,
    @JsonProperty("segment_ids")
    val segmentIds: List<String>? = null,
    @JsonProperty("passenger_ids")
    val passengerIds: List<String>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelPrivateFare(
    @JsonProperty("corporate_code")
    val corporateCode: String? = null,
    @JsonProperty("tracking_reference")
    val trackingReference: String? = null,
    @JsonProperty("tour_code")
    val tourCode: String? = null,
)