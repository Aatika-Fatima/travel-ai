package com.travel.duffel.internal.client

import com.travel.common.exception.BookingException
import com.travel.common.exception.BookingFailureReason
import com.travel.common.exception.ExternalApiException
import com.travel.common.exception.ValidationException
import com.travel.duffel.api.dto.request.DuffelOrderRequestEnvelope
import com.travel.duffel.api.dto.request.DuffelOrderRequestPayload
import com.travel.duffel.api.dto.response.DuffelErrorResponse
import com.travel.duffel.api.dto.response.DuffelOffer
import com.travel.duffel.api.dto.response.DuffelOfferResponse
import com.travel.duffel.api.dto.response.DuffelOrderData
import com.travel.duffel.api.dto.response.DuffelOrderListResponse
import com.travel.duffel.api.dto.response.DuffelOrderResponse
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.json.JsonMapper

@Component
class DuffelOrderClient(
    private val duffelRestClient: RestClient,
    private val jsonMapper: JsonMapper,
) {
    @Retryable(
        exceptionExpression = "#root instanceof T(com.travel.common.exception.ExternalApiException) && #root.retryable",
        maxAttempts = 3,
        backoff = Backoff(delay = 500, multiplier = 2.0),
    )
    fun fetchOffer(offerId: String): DuffelOffer {
        val response =
            try {
                duffelRestClient
                    .get()
                    .uri("/air/offers/{id}", offerId)
                    .retrieve()
                    .body(DuffelOfferResponse::class.java)
            } catch (ex: RestClientResponseException) {
                throw reclassifyOfferError(ex)
            } catch (ex: RestClientException) {
                throw ExternalApiException(
                    "Duffel API request timed out or was unreachable",
                    retryable = true,
                    cause = ex,
                )
            }

        return response?.data ?: throw ExternalApiException("Duffel API returned an empty offer response")
    }

    // The Idempotency-Key must be the SAME value across every retry attempt of this logical
    // booking call (generated once by the caller, not here) - a fresh key per attempt would
    // let a retried request create a second, duplicate order if the first actually succeeded
    // but the response was lost (e.g. a timeout).
    @Retryable(
        exceptionExpression = "#root instanceof T(com.travel.common.exception.ExternalApiException) && #root.retryable",
        maxAttempts = 3,
        backoff = Backoff(delay = 500, multiplier = 2.0),
    )
    fun createOrder(
        payload: DuffelOrderRequestPayload,
        idempotencyKey: String,
    ): DuffelOrderData {
        val response =
            try {
                duffelRestClient
                    .post()
                    .uri("/air/orders")
                    .header("Idempotency-Key", idempotencyKey)
                    .body(DuffelOrderRequestEnvelope(payload))
                    .retrieve()
                    .body(DuffelOrderResponse::class.java)
            } catch (ex: RestClientResponseException) {
                throw reclassifyOrderError(ex)
            } catch (ex: RestClientException) {
                throw ExternalApiException(
                    "Duffel API request timed out or was unreachable",
                    retryable = true,
                    cause = ex,
                )
            }

        return response?.data ?: throw ExternalApiException("Duffel API returned an empty order response")
    }

    // Added alongside fetchOffer()/createOrder() -- same retry/error-mapping
    // shape as both. Backs ReconciliationJob's sweep (order-service §P8):
    // "has an order already been created against this internal_order_id."
    @Retryable(
        exceptionExpression = "#root instanceof T(com.travel.common.exception.ExternalApiException) && #root.retryable",
        maxAttempts = 3,
        backoff = Backoff(delay = 500, multiplier = 2.0),
    )
    fun findOrdersByMetadata(key: String, value: String): List<DuffelOrderData> {
        val response =
            try {
                duffelRestClient
                    .get()
                    .uri { builder ->
                        builder.path("/air/orders")
                            .queryParam("metadata[$key]", value)
                            .build()
                    }
                    .retrieve()
                    .body(DuffelOrderListResponse::class.java)
            } catch (ex: RestClientResponseException) {
                throw reclassifyOrderError(ex)
            } catch (ex: RestClientException) {
                throw ExternalApiException(
                    "Duffel API request timed out or was unreachable",
                    retryable = true,
                    cause = ex,
                )
            }

        return response?.data ?: emptyList()
    }

    private fun reclassifyOfferError(ex: RestClientResponseException): RuntimeException {
        val base = DuffelErrorMapper.toDomainException(ex, jsonMapper)
        return if (base is ExternalApiException && (base.statusCode == 404 || base.statusCode == 410)) {
            BookingException(
                "This offer is no longer available. Please search again.",
                reason = BookingFailureReason.OFFER_EXPIRED,
                cause = base,
            )
        } else {
            base
        }
    }

    private fun reclassifyOrderError(ex: RestClientResponseException): RuntimeException {
        val base = DuffelErrorMapper.toDomainException(ex, jsonMapper)
        val errors = parseErrors(ex.responseBodyAsString)

        val isPaymentRelated =
            errors.any { error ->
                val haystack = listOfNotNull(error.type, error.code).joinToString(" ").lowercase()
                haystack.contains("payment") || haystack.contains("balance")
            }

        return when {
            isPaymentRelated ->
                BookingException(
                    "Payment was declined.",
                    reason = BookingFailureReason.PAYMENT_DECLINED,
                    cause = base,
                )
            base is ValidationException ->
                BookingException(
                    "Passenger details were rejected: ${base.errors.joinToString().ifBlank { base.message.orEmpty() }}",
                    reason = BookingFailureReason.VALIDATION_FAILED,
                    fieldErrors =
                        errors
                            .mapNotNull { error -> error.source?.field?.let { field -> field to (error.message ?: error.title ?: "invalid") } }
                            .toMap(),
                    cause = base,
                )
            else -> base
        }
    }

    private fun parseErrors(body: String) =
        runCatching {
            jsonMapper.readValue(body, DuffelErrorResponse::class.java).errors
        }.getOrDefault(emptyList())
}
