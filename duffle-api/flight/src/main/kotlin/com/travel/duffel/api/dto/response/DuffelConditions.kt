package com.travel.duffel.api.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.annotation.JsonDeserialize

// Duffel sends either a detail object or a bare `false` when the condition isn't offered.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = DuffelConditionDetailDeserializer::class)
data class DuffelConditionDetail(
    val allowed: Boolean = false,
    @JsonProperty("penalty_amount")
    val penaltyAmount: String? = null,
    @JsonProperty("penalty_currency")
    val penaltyCurrency: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelOfferConditions(
    @JsonProperty("refund_before_departure")
    val refundBeforeDeparture: DuffelConditionDetail? = null,
    @JsonProperty("change_before_departure")
    val changeBeforeDeparture: DuffelConditionDetail? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelSliceConditions(
    @JsonProperty("change_before_departure")
    val changeBeforeDeparture: DuffelConditionDetail? = null,
    @JsonProperty("priority_check_in")
    val priorityCheckIn: DuffelConditionDetail? = null,
    @JsonProperty("priority_boarding")
    val priorityBoarding: DuffelConditionDetail? = null,
    @JsonProperty("advance_seat_selection")
    val advanceSeatSelection: DuffelConditionDetail? = null,
)