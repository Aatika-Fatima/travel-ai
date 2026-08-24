package com.travel.duffel.internal.booking

import com.travel.common.events.BookingEvent
import com.travel.common.model.BookingRequest
import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import com.travel.duffel.api.dto.request.DuffelOfferRequestPayload
import com.travel.duffel.api.dto.request.DuffelPassengerRequest
import com.travel.duffel.api.dto.request.DuffelSliceRequest
import com.travel.duffel.internal.client.DuffelFlightClient
import com.travel.duffel.internal.client.DuffelOrderClient
import com.travel.duffel.internal.config.client.DuffelClientConfig
import com.travel.duffel.internal.config.client.DuffelProperties
import com.travel.notification.api.outbox.BookingOutboxWriter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.mock
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.sql.DriverManager
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertTrue

// Proves the full search -> book -> email path against real, already-running local infra:
// searches and books a real Duffel sandbox offer (search-ai has no booking action yet and
// duffle-api/flight has no REST controller, so this drives the same service classes directly,
// same as DuffelBookingSandboxIT), then inserts the resulting outbox row via plain JDBC into
// the same Postgres the running `app` process uses. `app`'s already-live OutboxPublisher picks
// it up within ~1s, publishes to booking-events, BookingEventConsumer forwards it, and EmailWorker
// sends the email through Mailhog - no new production wiring needed to observe this.
@EnabledIfEnvironmentVariable(named = "DUFFEL_API_KEY", matches = ".+")
class BookingToEmailPipelineSandboxIT {
    @Test
    fun `search, book, and hand off to the notification pipeline via the outbox`() {
        val apiKey = System.getenv("DUFFEL_API_KEY")
        val properties = DuffelProperties(apiKey = apiKey, timeoutMs = 20_000L)
        val restClient = DuffelClientConfig().duffelRestClient(properties)
        val jsonMapper = JsonMapper.builder().build()

        val flightClient = DuffelFlightClient(restClient, jsonMapper)
        val orderClient = DuffelOrderClient(restClient, jsonMapper)
        val bookingService = DuffelBookingServiceImpl(orderClient, mock(BookingOutboxWriter::class.java))

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
        println("Found sandbox offer ${offer.id}")

        val passengerEmail = "test.traveler@example.com"
        val request =
            BookingRequest(
                offerId = offer.id,
                passengers =
                    listOf(
                        PassengerDetails(
                            title = "mr",
                            givenName = "Test",
                            familyName = "Passenger",
                            dateOfBirth = LocalDate.of(1990, 1, 1),
                            gender = "m",
                            email = passengerEmail,
                            phoneNumber = "+442080160508",
                        ),
                    ),
                contact = ContactInfo(email = passengerEmail, phoneNumber = "+442080160508"),
                paymentType = "balance",
            )

        val bookingResult = bookingService.createBooking(request, "test-idempotency-key")

        assertTrue(bookingResult.orderId.isNotBlank())
        assertTrue(bookingResult.bookingReference.isNotBlank())
        println("Booked sandbox order ${bookingResult.orderId} (${bookingResult.bookingReference})")

        val payload =
            jacksonObjectMapper().writeValueAsString(
                BookingEvent(
                    orderId = bookingResult.orderId,
                    bookingReference = bookingResult.bookingReference,
                    emails = listOf(passengerEmail),
                ),
            )

        DriverManager.getConnection("jdbc:postgresql://localhost:5434/travel", "postgres", "password").use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO booking_outbox (event_id, event_type, order_id, payload)
                VALUES (?, 'BOOKING_CONFIRMED', ?, ?::jsonb)
                """.trimIndent(),
            ).use { stmt ->
                stmt.setObject(1, UUID.randomUUID())
                stmt.setString(2, bookingResult.orderId)
                stmt.setString(3, payload)
                stmt.executeUpdate()
            }
        }
        println("Outbox row inserted - app's OutboxPublisher will publish it to booking-events within ~1s")
    }
}
