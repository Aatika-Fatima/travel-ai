package com.travel.duffel.client

import com.travel.common.exception.ExternalApiException
import com.travel.duffel.dto.response.DuffelAirportsResponse
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.json.JsonMapper

@Component
class DuffelAirportClient(
    private val duffelRestClient: RestClient,
    private val jsonMapper: JsonMapper,
) {
    @Retryable(
        exceptionExpression = "#root instanceof T(com.travel.common.exception.ExternalApiException) && #root.retryable",
        maxAttempts = 3,
        backoff = Backoff(delay = 500, multiplier = 2.0),
    )
    fun fetchAirports(
        after: String? = null,
        limit: Int = 50,
        iataCountryCode: String? = null,
    ): DuffelAirportsResponse {
        val response =
            try {
                duffelRestClient
                    .get()
                    .uri { builder ->
                        builder.path("/air/airports").queryParam("limit", limit)
                        after?.let { builder.queryParam("after", it) }
                        iataCountryCode?.let { builder.queryParam("iata_country_code", it) }
                        builder.build()
                    }.retrieve()
                    .body(DuffelAirportsResponse::class.java)
            } catch (ex: RestClientResponseException) {
                throw DuffelErrorMapper.toDomainException(ex, jsonMapper)
            } catch (ex: RestClientException) {
                throw ExternalApiException(
                    "Duffel API request timed out or was unreachable",
                    retryable = true,
                    cause = ex,
                )
            }

        return response ?: throw ExternalApiException("Duffel API returned an empty airports page")
    }
}
