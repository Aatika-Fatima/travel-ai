package com.travel.duffel.client

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import com.travel.common.exception.ExternalApiException
import com.travel.common.exception.ValidationException
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

class DuffelAirportClientTest {
    companion object {
        @RegisterExtension
        @JvmStatic
        val wireMock: WireMockExtension = WireMockExtension.newInstance().build()

        private const val PAGE_BODY =
            """
            {
              "data": [
                {
                  "id": "arp_lhr_gb",
                  "name": "Heathrow",
                  "iata_code": "LHR",
                  "icao_code": "EGLL",
                  "iata_city_code": "LON",
                  "city_name": "London",
                  "iata_country_code": "GB",
                  "latitude": 51.4700223,
                  "longitude": -0.4542955,
                  "time_zone": "Europe/London"
                }
              ],
              "meta": {"after": "cursor-2", "limit": 50}
            }
            """
    }

    private fun client(timeoutMs: Long = 2000): DuffelAirportClient = DuffelAirportClient(restClient(timeoutMs), JsonMapper.builder().build())

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
    fun `returns a page of airports on success`() {
        wireMock.stubFor(get(urlPathEqualTo("/air/airports")).willReturn(okJson(PAGE_BODY)))

        val page = client().fetchAirports()

        assertEquals(1, page.data.size)
        assertEquals("LHR", page.data.first().iataCode)
        assertEquals("cursor-2", page.meta.after)
    }

    @Test
    fun `maps 422 response to ValidationException`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/air/airports"))
                .willReturn(
                    aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"errors":[{"title":"invalid_input","message":"iata_country_code is invalid"}]}"""),
                ),
        )

        val ex = assertFailsWith<ValidationException> { client().fetchAirports(iataCountryCode = "??") }

        assertTrue(ex.errors.any { it.contains("iata_country_code is invalid") })
    }

    @Test
    fun `exhausts retries on repeated 429 and throws ExternalApiException`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/air/airports"))
                .willReturn(
                    aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"errors":[{"title":"rate_limited"}]}"""),
                ),
        )

        val ex = assertFailsWith<ExternalApiException> { retryableClient().fetchAirports() }

        assertEquals(429, ex.statusCode)
        wireMock.verify(3, getRequestedFor(urlPathEqualTo("/air/airports")))
    }

    @Test
    fun `recovers after a transient 429 via retry`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/air/airports"))
                .inScenario("retry-recovery")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("recovered"),
        )
        wireMock.stubFor(
            get(urlPathEqualTo("/air/airports"))
                .inScenario("retry-recovery")
                .whenScenarioStateIs("recovered")
                .willReturn(okJson(PAGE_BODY)),
        )

        val page = retryableClient().fetchAirports()

        assertEquals(1, page.data.size)
    }

    // A plain `DuffelAirportClient(...)` instance bypasses Spring's retry AOP advice
    // entirely, so retry behavior can only be observed through a real proxy.
    private fun retryableClient(): DuffelAirportClient {
        val context = AnnotationConfigApplicationContext(RetryTestConfig::class.java)
        return context.getBean(DuffelAirportClient::class.java)
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
        fun duffelAirportClient(
            restClient: RestClient,
            jsonMapper: JsonMapper,
        ): DuffelAirportClient = DuffelAirportClient(restClient, jsonMapper)
    }
}
