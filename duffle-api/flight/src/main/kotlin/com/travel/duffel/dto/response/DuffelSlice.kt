package com.travel.duffel.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelSlice(
    val id: String? = null,
    val origin: DuffelPlace,
    val destination: DuffelPlace,
    @JsonProperty("origin_type")
    val originType: String? = null,
    @JsonProperty("destination_type")
    val destinationType: String? = null,
    val duration: String? = null,
    @JsonProperty("fare_brand_name")
    val fareBrandName: String? = null,
    @JsonProperty("comparison_key")
    val comparisonKey: String? = null,
    val conditions: DuffelSliceConditions? = null,
    val segments: List<DuffelSegment>,
)