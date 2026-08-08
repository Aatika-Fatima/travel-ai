package com.travel.duffel.booking

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.travel.common.exception.BookingException
import com.travel.common.exception.BookingFailureReason
import com.travel.common.model.BookingRequest
import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import com.travel.duffel.client.DuffelOrderClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DuffelBookingServiceImplTest {
    companion object {
        @RegisterExtension
        @JvmStatic
        val wireMock: WireMockExtension = WireMockExtension.newInstance().build()

        private fun offerBody(expiresAt: String? = null) =
            """
            {
              "data": {
                "id": "off_123",
                "live_mode": false,
                "partial": false,
                "passenger_identity_documents_required": false,
                ${expiresAt?.let { """"expires_at": "$it",""" } ?: ""}
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

    private fun service(): DuffelBookingServiceImpl {
        val timeout = Duration.ofSeconds(2)
        val requestFactory =
            ClientHttpRequestFactoryBuilder
                .simple()
                .build(HttpClientSettings.defaults().withTimeouts(timeout, timeout))
        val restClient =
            RestClient
                .builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build()
        val orderClient = DuffelOrderClient(restClient, JsonMapper.builder().build())
        return DuffelBookingServiceImpl(orderClient)
    }

    private fun request(offerPassengerId: String = "pas_123") =
        BookingRequest(
            offerId = "off_123",
            passengers =
                listOf(
                    PassengerDetails(
                        offerPassengerId = offerPassengerId,
                        title = "mr",
                        givenName = "Ada",
                        familyName = "Lovelace",
                        dateOfBirth = LocalDate.of(1990, 1, 1),
                        gender = "f",
                        email = "ada@example.com",
                        phoneNumber = "+10000000000",
                    ),
                ),
            contact = ContactInfo(email = "ada@example.com", phoneNumber = "+10000000000"),
        )

    @Test
    fun `books the offer end-to-end and maps the result`() {
        wireMock.stubFor(get(urlPathEqualTo("/air/offers/off_123")).willReturn(okJson(offerBody())))
        wireMock.stubFor(post(urlPathEqualTo("/air/orders")).willReturn(okJson(ORDER_BODY)))

        val result = service().createBooking(request())

        assertEquals("ord_123", result.orderId)
        assertEquals("ABC123", result.bookingReference)
        assertEquals(listOf("1234567890"), result.eTicketNumbers)
        assertEquals(199.99, result.totalAmount.amount)
        assertEquals("USD", result.totalAmount.currency)
    }

    @Test
    fun `refuses to book an already-expired offer without calling Duffel`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/air/offers/off_123"))
                .willReturn(okJson(offerBody(expiresAt = "2000-01-01T00:00:00Z"))),
        )

        val ex = assertFailsWith<BookingException> { service().createBooking(request()) }

        assertEquals(BookingFailureReason.OFFER_EXPIRED, ex.reason)
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/air/orders")))
    }

    @Test
    fun `rejects a passenger id that does not match the offer without calling Duffel`() {
        wireMock.stubFor(get(urlPathEqualTo("/air/offers/off_123")).willReturn(okJson(offerBody())))

        val ex = assertFailsWith<BookingException> { service().createBooking(request(offerPassengerId = "pas_unknown")) }

        assertEquals(BookingFailureReason.VALIDATION_FAILED, ex.reason)
        assertEquals("does not match any passenger on this offer", ex.fieldErrors["passengers[0].offerPassengerId"])
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/air/orders")))
    }

    @Test
    fun `propagates a Duffel-side validation failure as VALIDATION_FAILED`() {
        wireMock.stubFor(get(urlPathEqualTo("/air/offers/off_123")).willReturn(okJson(offerBody())))
        wireMock.stubFor(
            post(urlPathEqualTo("/air/orders"))
                .willReturn(
                    aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"errors":[{"type":"invalid_input","message":"is required","source":{"field":"passengers[0].email"}}]}"""),
                ),
        )

        val ex = assertFailsWith<BookingException> { service().createBooking(request()) }

        assertEquals(BookingFailureReason.VALIDATION_FAILED, ex.reason)
    }

    @Test
    fun `propagates a payment decline as PAYMENT_DECLINED`() {
        wireMock.stubFor(get(urlPathEqualTo("/air/offers/off_123")).willReturn(okJson(offerBody())))
        wireMock.stubFor(
            post(urlPathEqualTo("/air/orders"))
                .willReturn(
                    aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"errors":[{"type":"payments","code":"insufficient_balance"}]}"""),
                ),
        )

        val ex = assertFailsWith<BookingException> { service().createBooking(request()) }

        assertEquals(BookingFailureReason.PAYMENT_DECLINED, ex.reason)
    }

    @Test
    fun `propagates an expired offer from Duffel as OFFER_EXPIRED`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/air/offers/off_123")).willReturn(aResponse().withStatus(410)),
        )

        val ex = assertFailsWith<BookingException> { service().createBooking(request()) }

        assertEquals(BookingFailureReason.OFFER_EXPIRED, ex.reason)
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/air/orders")))
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/air/offers/off_123")))
    }
}
