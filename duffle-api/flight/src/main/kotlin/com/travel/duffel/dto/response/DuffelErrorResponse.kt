package com.travel.duffel.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelErrorResponse(
    val errors: List<DuffelError> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelError(
    val type: String? = null,
    val title: String? = null,
    val message: String? = null,
    val code: String? = null,
    val source: DuffelErrorSource? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelErrorSource(
    val field: String? = null,
    val pointer: String? = null,
)