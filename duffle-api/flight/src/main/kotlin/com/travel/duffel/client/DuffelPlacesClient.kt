package com.travel.duffel.client

import com.travel.common.exception.ExternalApiException
import com.travel.duffel.dto.response.DuffelPlace
import com.travel.duffel.dto.response.DuffelPlacesResponse
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.json.JsonMapper

@Component
class DuffelPlacesClient(
    private val duffelRestClient: RestClient,
    private val jsonMapper: JsonMapper,
) {
    @Retryable(
        exceptionExpression = "#root instanceof T(com.travel.common.exception.ExternalApiException) && #root.retryable",
        maxAttempts = 3,
        backoff = Backoff(delay = 500, multiplier = 2.0),
    )
    fun suggest(query: String): List<DuffelPlace> {
        val response =
            try {
                duffelRestClient
                    .get()
                    .uri { builder ->
                        builder.path("/places/suggestions").queryParam("query", query).build()
                    }.retrieve()
                    .body(DuffelPlacesResponse::class.java)
            } catch (ex: RestClientResponseException) {
                throw DuffelErrorMapper.toDomainException(ex, jsonMapper)
            } catch (ex: RestClientException) {
                throw ExternalApiException(
                    "Duffel API request timed out or was unreachable",
                    retryable = true,
                    cause = ex,
                )
            }

        return response?.data ?: emptyList()
    }
}
