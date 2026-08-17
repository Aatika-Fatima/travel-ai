package com.travel.duffel.internal.client

import com.travel.common.exception.ExternalApiException
import com.travel.duffel.api.dto.request.DuffelOfferRequestEnvelope
import com.travel.duffel.api.dto.request.DuffelOfferRequestPayload
import com.travel.duffel.api.dto.response.DuffelOffer
import com.travel.duffel.api.dto.response.DuffelOfferRequestResponse
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
                throw DuffelErrorMapper.toDomainException(ex, jsonMapper)
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
}
