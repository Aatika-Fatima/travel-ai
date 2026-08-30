package com.travel.duffel.api.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

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
    // Duffel sends `source` as an object ({field, pointer}) for validation
    // errors but as a bare string ("travelport", "airline") for airline /
    // provider errors -- binding it straight to DuffelErrorSource made the
    // whole response fail to deserialize on the airline-error path, which
    // is exactly the path we most need `code` from. Take it as Any and
    // narrow only when it's the object form.
    @JsonProperty("source")
    val sourceRaw: Any? = null,
) {
    @get:JsonIgnore
    val source: DuffelErrorSource?
        get() = (sourceRaw as? Map<*, *>)?.let {
            DuffelErrorSource(field = it["field"] as? String, pointer = it["pointer"] as? String)
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DuffelErrorSource(
    val field: String? = null,
    val pointer: String? = null,
)
