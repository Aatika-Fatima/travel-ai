package com.travel.duffel.client

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import com.travel.common.exception.ExternalApiException
import com.travel.common.exception.ValidationException
import com.travel.duffel.dto.request.DuffelOfferRequestPayload
import com.travel.duffel.dto.request.DuffelPassengerRequest
import com.travel.duffel.dto.request.DuffelSliceRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.retry.annotation.EnableRetry
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DuffelFlightClientTest {
    companion object {
        @RegisterExtension
        @JvmStatic
        val wireMock: WireMockExtension = WireMockExtension.newInstance().build()

        private val SUCCESS_BODY =
            """
            {
              "data": {
                "id": "orq_123",
                "offers": [
                  {
                    "id": "off_123",
                    "live_mode": false,
                    "partial": false,
                    "passenger_identity_documents_required": false,
                    "total_amount": "199.99",
                    "total_currency": "USD",
                    "owner": {"name": "Test Air", "iata_code": "TA"},
                    "slices": [
                      {
                        "origin": {"iata_code": "DEL", "name": "Indira Gandhi Intl"},
                        "destination": {"iata_code": "BOM", "name": "Chhatrapati Shivaji"},
                        "duration": "PT2H5M",
                        "segments": [
                          {
                            "origin": {"iata_code": "DEL", "name": "Indira Gandhi Intl"},
                            "destination": {"iata_code": "BOM", "name": "Chhatrapati Shivaji"},
                            "departing_at": "2026-09-01T10:00:00",
                            "arriving_at": "2026-09-01T12:05:00",
                            "duration": "PT2H5M",
                            "marketing_carrier": {"name": "Test Air", "iata_code": "TA"},
                            "marketing_carrier_flight_number": "101"
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()
    }

    private fun samplePayload() =
        DuffelOfferRequestPayload(
            slices = listOf(DuffelSliceRequest(origin = "DEL", destination = "BOM", departureDate = "2026-09-01")),
            passengers = listOf(DuffelPassengerRequest(type = "adult")),
        )

    private fun client(timeoutMs: Long = 2000): DuffelFlightClient = DuffelFlightClient(restClient(timeoutMs), JsonMapper.builder().build())

    private fun restClient(timeoutMs: Long): RestClient {
        val timeout = Duration.ofMillis(timeoutMs)
        val requestFactory =
            ClientHttpRequestFactoryBuilder
                .simple()
                .build(HttpClientSettings.defaults().withTimeouts(timeout, timeout))
        return RestClient
            .builder()
            .baseUrl(wireMock.baseUrl())
            .requestFactory(requestFactory)
            .defaultHeader("Authorization", "Bearer test-key")
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    @Test
    fun `returns offers on success`() {
        wireMock.stubFor(post(urlPathEqualTo("/air/offer_requests")).willReturn(okJson(SUCCESS_BODY)))

        val offers = client().createOfferRequest(samplePayload())

        assertEquals(1, offers.size)
        assertEquals("off_123", offers.first().id)
    }

    @Test
    fun `returns empty list when no offers found`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/air/offer_requests"))
                .willReturn(okJson("""{"data":{"id":"orq_123","offers":[]}}""")),
        )

        val offers = client().createOfferRequest(samplePayload())

        assertTrue(offers.isEmpty())
    }

    @Test
    fun `maps 422 response to ValidationException`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/air/offer_requests"))
                .willReturn(
                    aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"errors":[{"title":"invalid_input","message":"origin is required"}]}"""),
                ),
        )

        val ex = assertFailsWith<ValidationException> { client().createOfferRequest(samplePayload()) }

        assertTrue(ex.errors.any { it.contains("origin is required") })
    }

    @Test
    fun `exhausts retries on repeated 429 and throws ExternalApiException`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/air/offer_requests"))
                .willReturn(
                    aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"errors":[{"title":"rate_limited"}]}"""),
                ),
        )

        val ex = assertFailsWith<ExternalApiException> { retryableClient().createOfferRequest(samplePayload()) }

        assertEquals(429, ex.statusCode)
        wireMock.verify(3, postRequestedFor(urlPathEqualTo("/air/offer_requests")))
    }

    @Test
    fun `recovers after a transient 429 via retry`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/air/offer_requests"))
                .inScenario("retry-recovery")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("recovered"),
        )
        wireMock.stubFor(
            post(urlPathEqualTo("/air/offer_requests"))
                .inScenario("retry-recovery")
                .whenScenarioStateIs("recovered")
                .willReturn(okJson(SUCCESS_BODY)),
        )

        val offers = retryableClient().createOfferRequest(samplePayload())

        assertEquals(1, offers.size)
    }

    @Test
    fun `times out when Duffel is slow to respond`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/air/offer_requests"))
                .willReturn(okJson(SUCCESS_BODY).withFixedDelay(1500)),
        )

        val ex = assertFailsWith<ExternalApiException> { client(timeoutMs = 300).createOfferRequest(samplePayload()) }

        assertTrue(ex.retryable)
    }

    // A plain `DuffelFlightClient(...)` instance bypasses Spring's retry AOP advice
    // entirely, so retry behavior can only be observed through a real proxy.
    private fun retryableClient(): DuffelFlightClient {
        val context = AnnotationConfigApplicationContext(RetryTestConfig::class.java)
        return context.getBean(DuffelFlightClient::class.java)
    }

    @Configuration
    @EnableRetry
    class RetryTestConfig {
        @Bean
        fun jsonMapper(): JsonMapper = JsonMapper.builder().build()

        @Bean
        fun restClient(): RestClient {
            val timeout = Duration.ofSeconds(2)
            val requestFactory =
                ClientHttpRequestFactoryBuilder
                    .simple()
                    .build(HttpClientSettings.defaults().withTimeouts(timeout, timeout))
            return RestClient
                .builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build()
        }

        @Bean
        fun duffelFlightClient(
            restClient: RestClient,
            jsonMapper: JsonMapper,
        ): DuffelFlightClient = DuffelFlightClient(restClient, jsonMapper)
    }
}
