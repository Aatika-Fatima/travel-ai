package com.travel.duffel.client

import com.travel.common.exception.ExternalApiException
import com.travel.common.exception.ValidationException
import com.travel.duffel.dto.response.DuffelErrorResponse
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.json.JsonMapper

object DuffelErrorMapper {
    fun toDomainException(
        ex: RestClientResponseException,
        jsonMapper: JsonMapper,
    ): RuntimeException {
        val status = ex.statusCode
        val errorMessages = parseErrorMessages(ex.responseBodyAsString, jsonMapper)

        return when {
            status == HttpStatusCode.valueOf(400) || status == HttpStatusCode.valueOf(422) ->
                ValidationException(
                    "Duffel rejected the search request: ${errorMessages.joinToString().ifBlank { ex.message.orEmpty() }}",
                    errors = errorMessages,
                )
            status == HttpStatusCode.valueOf(429) ->
                ExternalApiException(
                    "Duffel API rate limit exceeded",
                    statusCode = status.value(),
                    retryable = true,
                    cause = ex,
                )
            status.is5xxServerError ->
                ExternalApiException(
                    "Duffel API server error (${status.value()})",
                    statusCode = status.value(),
                    retryable = true,
                    cause = ex,
                )
            else ->
                ExternalApiException(
                    "Duffel API request failed (${status.value()}): ${ex.responseBodyAsString}",
                    statusCode = status.value(),
                    retryable = false,
                    cause = ex,
                )
        }
    }

    private fun parseErrorMessages(
        body: String,
        jsonMapper: JsonMapper,
    ): List<String> =
        runCatching {
            jsonMapper.readValue(body, DuffelErrorResponse::class.java).errors.mapNotNull { it.message ?: it.title }
        }.getOrDefault(emptyList())
}
