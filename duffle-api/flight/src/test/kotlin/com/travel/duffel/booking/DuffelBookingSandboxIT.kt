package com.travel.duffel.booking

import com.travel.common.model.BookingRequest
import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import com.travel.duffel.client.DuffelFlightClient
import com.travel.duffel.client.DuffelOrderClient
import com.travel.duffel.config.DuffelClientConfig
import com.travel.duffel.config.DuffelProperties
import com.travel.duffel.dto.request.DuffelOfferRequestPayload
import com.travel.duffel.dto.request.DuffelPassengerRequest
import com.travel.duffel.dto.request.DuffelSliceRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import kotlin.test.assertTrue

// Exercises a real booking against Duffel's sandbox API (search -> re-fetch -> order),
// using the `balance` payment type sandbox accounts are seeded with test funds for. Only
// runs when a real DUFFEL_API_KEY is present in the environment - skipped everywhere else
// (e.g. CI without secrets) rather than failing.
@EnabledIfEnvironmentVariable(named = "DUFFEL_API_KEY", matches = ".+")
class DuffelBookingSandboxIT {
    @Test
    fun `books a real sandbox offer end-to-end`() {
        val apiKey = System.getenv("DUFFEL_API_KEY")
        val properties = DuffelProperties(apiKey = apiKey, timeoutMs = 20_000L)
        val restClient = DuffelClientConfig().duffelRestClient(properties)
        val jsonMapper = JsonMapper.builder().build()

        val flightClient = DuffelFlightClient(restClient, jsonMapper)
        val orderClient = DuffelOrderClient(restClient, jsonMapper)
        val bookingService = DuffelBookingServiceImpl(orderClient)

        val departureDate = LocalDate.now().plusDays(30).toString()
        val offers =
            flightClient.createOfferRequest(
                DuffelOfferRequestPayload(
                    slices = listOf(DuffelSliceRequest(origin = "LHR", destination = "JFK", departureDate = departureDate)),
                    passengers = listOf(DuffelPassengerRequest(type = "adult")),
                ),
            )
        check(offers.isNotEmpty()) { "Sandbox search returned no offers - cannot exercise booking" }
        val offer = offers.first()
        val offerPassengerId =
            offer.passengers?.firstOrNull()?.id
                ?: error("Sandbox offer had no passenger id to book against")

        val request =
            BookingRequest(
                offerId = offer.id,
                passengers =
                    listOf(
                        PassengerDetails(
                            offerPassengerId = offerPassengerId,
                            title = "mr",
                            givenName = "Test",
                            familyName = "Passenger",
                            dateOfBirth = LocalDate.of(1990, 1, 1),
                            gender = "m",
                            email = "test@example.com",
                            phoneNumber = "+442080160508",
                        ),
                    ),
                contact = ContactInfo(email = "test@example.com", phoneNumber = "+442080160508"),
                paymentType = "balance",
            )

        val result = bookingService.createBooking(request)

        assertTrue(result.orderId.isNotBlank())
        assertTrue(result.bookingReference.isNotBlank())
        println("Booked sandbox order ${result.orderId} (${result.bookingReference}), status=${result.status}")
    }
}
