package com.travel.duffel.client

import com.travel.common.exception.ExternalApiException
import com.travel.common.exception.ValidationException
import com.travel.duffel.dto.request.DuffelOfferRequestEnvelope
import com.travel.duffel.dto.request.DuffelOfferRequestPayload
import com.travel.duffel.dto.response.DuffelErrorResponse
import com.travel.duffel.dto.response.DuffelOffer
import com.travel.duffel.dto.response.DuffelOfferRequestResponse
import org.springframework.http.HttpStatusCode
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Component
class DuffelFlightClient(
    private val duffelRestClient: RestClient,
    private val jsonMapper: JsonMapper,
) {
    @Retryable(
        exceptionExpression = "#root instanceof T(com.travel.common.exception.ExternalApiException) && #root.retryable",
        maxAttempts = 3,
        backoff = Backoff(delay = 500, multiplier = 2.0),
    )
    fun createOfferRequest(payload: DuffelOfferRequestPayload): List<DuffelOffer> {
        val response =
            try {
                duffelRestClient
                    .post()
                    .uri { builder ->
                        builder.path("/air/offer_requests").queryParam("return_offers", true)
                        payload.currency?.let { builder.queryParam("currency", it) }
                        builder.build()
                    }.header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(DuffelOfferRequestEnvelope(payload))
                    .retrieve()
                    .body(DuffelOfferRequestResponse::class.java)
            } catch (ex: RestClientResponseException) {
                throw toDomainException(ex)
            } catch (ex: RestClientException) {
                throw ExternalApiException(
                    "Duffel API request timed out or was unreachable",
                    retryable = true,
                    cause = ex,
                )
            }

        return response?.data?.offers
            ?: throw ExternalApiException("Duffel API returned an empty response")
    }

    private fun toDomainException(ex: RestClientResponseException): RuntimeException {
        val status = ex.statusCode
        val errorMessages = parseErrorMessages(ex.responseBodyAsString)

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

    private fun parseErrorMessages(body: String): List<String> =
        runCatching {
            jsonMapper.readValue(body, DuffelErrorResponse::class.java).errors.mapNotNull { it.message ?: it.title }
        }.getOrDefault(emptyList())
}
