package com.travel.duffel.api.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

// Duffel's list endpoints wrap results in the same "data" envelope as the
// single-resource responses (DuffelOrderResponse) -- just a list instead of
// one object.
@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelOrderListResponse(
    val data: List<DuffelOrderData> = emptyList(),
)
