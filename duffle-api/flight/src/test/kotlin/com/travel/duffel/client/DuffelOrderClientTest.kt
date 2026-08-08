package com.travel.duffel.client

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import com.travel.common.exception.BookingException
import com.travel.common.exception.BookingFailureReason
import com.travel.duffel.dto.request.DuffelOrderPassengerRequest
import com.travel.duffel.dto.request.DuffelOrderRequestPayload
import com.travel.duffel.dto.request.DuffelPaymentRequest
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
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DuffelOrderClientTest {
    companion object {
        @RegisterExtension
        @JvmStatic
        val wireMock: WireMockExtension = WireMockExtension.newInstance().build()

        private const val OFFER_BODY =
            """
            {
              "data": {
                "id": "off_123",
                "live_mode": false,
                "partial": false,
                "passenger_identity_documents_required": false,
                "total_amount": "199.99",
                "total_currency": "USD",
                "owner": {"name": "Test Air", "iata_code": "TA"},
                "passengers": [{"id": "pas_123", "given_name": "Ada", "family_name": "Lovelace"}],
                "slices": []
              }
            }
            """

        private const val ORDER_BODY =
            """
            {
              "data": {
                "id": "ord_123",
                "booking_reference": "ABC123",
                "status": "confirmed",
                "total_amount": "199.99",
                "total_currency": "USD",
                "documents": [{"type": "electronic_ticket", "unique_identifier": "1234567890"}],
                "passengers": [{"id": "pas_123", "given_name": "Ada", "family_name": "Lovelace"}]
              }
            }
            """
    }

    private fun client(timeoutMs: Long = 2000): DuffelOrderClient = DuffelOrderClient(restClient(timeoutMs), JsonMapper.builder().build())

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

    private fun samplePayload() =
        DuffelOrderRequestPayload(
            selectedOffers = listOf("off_123"),
            passengers =
                listOf(
                    DuffelOrderPassengerRequest(
                        id = "pas_123",
                        title = "mr",
                        givenName = "Ada",
                        familyName = "Lovelace",
                        bornOn = "1990-01-01",
                        gender = "f",
                        email = "ada@example.com",
                        phoneNumber = "+10000000000",
                    ),
                ),
            payments = listOf(DuffelPaymentRequest(type = "balance", currency = "USD", amount = "199.99")),
        )

    @Test
    fun `fetchOffer returns the offer on success`() {
        wireMock.stubFor(get(urlPathEqualTo("/air/offers/off_123")).willReturn(okJson(OFFER_BODY)))

        val offer = client().fetchOffer("off_123")

        assertEquals("off_123", offer.id)
        assertEquals("pas_123", offer.passengers?.first()?.id)
    }

    @Test
    fun `fetchOffer maps 404 to BookingException with OFFER_EXPIRED`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/air/offers/off_gone"))
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"errors":[{"title":"not_found"}]}"""),
                ),
        )

        val ex = assertFailsWith<BookingException> { client().fetchOffer("off_gone") }

        assertEquals(BookingFailureReason.OFFER_EXPIRED, ex.reason)
    }

    @Test
    fun `fetchOffer maps 410 to BookingException with OFFER_EXPIRED`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/air/offers/off_expired"))
                .willReturn(aResponse().withStatus(410)),
        )

        val ex = assertFailsWith<BookingException> { client().fetchOffer("off_expired") }

        assertEquals(BookingFailureReason.OFFER_EXPIRED, ex.reason)
    }

    @Test
    fun `createOrder returns the order on success`() {
        wireMock.stubFor(post(urlPathEqualTo("/air/orders")).willReturn(okJson(ORDER_BODY)))

        val order = client().createOrder(samplePayload(), UUID.randomUUID().toString())

        assertEquals("ord_123", order.id)
        assertEquals("ABC123", order.bookingReference)
        assertEquals("1234567890", order.documents?.first()?.uniqueIdentifier)
    }

    @Test
    fun `createOrder maps a payment-flavored 422 to BookingException with PAYMENT_DECLINED`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/air/orders"))
                .willReturn(
                    aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """{"errors":[{"type":"payments","code":"insufficient_balance","message":"Insufficient balance"}]}""",
                        ),
                ),
        )

        val ex = assertFailsWith<BookingException> { client().createOrder(samplePayload(), UUID.randomUUID().toString()) }

        assertEquals(BookingFailureReason.PAYMENT_DECLINED, ex.reason)
    }

    @Test
    fun `createOrder maps a passenger-flavored 422 to BookingException with VALIDATION_FAILED and field errors`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/air/orders"))
                .willReturn(
                    aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """{"errors":[{"type":"invalid_input","message":"is required","source":{"field":"passengers[0].email"}}]}""",
                        ),
                ),
        )

        val ex = assertFailsWith<BookingException> { client().createOrder(samplePayload(), UUID.randomUUID().toString()) }

        assertEquals(BookingFailureReason.VALIDATION_FAILED, ex.reason)
        assertEquals("is required", ex.fieldErrors["passengers[0].email"])
    }

    @Test
    fun `createOrder sends the same Idempotency-Key on every retried attempt`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/air/orders"))
                .inScenario("retry-recovery")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("recovered"),
        )
        wireMock.stubFor(
            post(urlPathEqualTo("/air/orders"))
                .inScenario("retry-recovery")
                .whenScenarioStateIs("recovered")
                .willReturn(okJson(ORDER_BODY)),
        )

        val key = UUID.randomUUID().toString()
        val order = retryableClient().createOrder(samplePayload(), key)

        assertEquals("ord_123", order.id)
        wireMock.verify(2, postRequestedFor(urlPathEqualTo("/air/orders")).withHeader("Idempotency-Key", matching(key)))
    }

    @Test
    fun `createOrder exhausts retries on repeated 429`() {
        wireMock.stubFor(
            post(urlPathEqualTo("/air/orders"))
                .willReturn(aResponse().withStatus(429)),
        )

        val key = UUID.randomUUID().toString()
        assertFailsWith<com.travel.common.exception.ExternalApiException> { retryableClient().createOrder(samplePayload(), key) }

        wireMock.verify(3, postRequestedFor(urlPathEqualTo("/air/orders")).withHeader("Idempotency-Key", equalTo(key)))
    }

    @Test
    fun `fetchOffer times out when Duffel is slow to respond`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/air/offers/off_slow"))
                .willReturn(okJson(OFFER_BODY).withFixedDelay(1500)),
        )

        val ex = assertFailsWith<com.travel.common.exception.ExternalApiException> { client(timeoutMs = 300).fetchOffer("off_slow") }

        assertTrue(ex.retryable)
    }

    // A plain `DuffelOrderClient(...)` instance bypasses Spring's retry AOP advice
    // entirely, so retry behavior can only be observed through a real proxy.
    private fun retryableClient(): DuffelOrderClient {
        val context = AnnotationConfigApplicationContext(RetryTestConfig::class.java)
        return context.getBean(DuffelOrderClient::class.java)
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
        fun duffelOrderClient(
            restClient: RestClient,
            jsonMapper: JsonMapper,
        ): DuffelOrderClient = DuffelOrderClient(restClient, jsonMapper)
    }
}
