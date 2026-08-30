package com.travel.duffel.internal.booking

import com.travel.common.events.BookingEvent
import com.travel.common.model.BookedPassenger
import com.travel.common.model.BookingRequest
import com.travel.common.model.BookingResult
import com.travel.common.model.Money
import com.travel.duffel.api.booking.DuffelBookingService
import com.travel.notification.api.outbox.BookingOutboxWriter
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.random.Random

/**
 * Active only under the `demo` profile, in place of [DuffelBookingServiceImpl].
 *
 * Duffel's Travelport sandbox withdraws an offer server-side within seconds of
 * the search that produced it (`offer_no_longer_available` on every real order
 * attempt), so a genuine end-to-end search -> book -> pay walkthrough against
 * the sandbox is impossible. This stub synthesises a confirmed booking -- no
 * Duffel call -- so the Kafka saga and the Razorpay payment flow can be
 * demonstrated. It still enqueues the BOOKING_CONFIRMED outbox event, so the
 * notification leg fires exactly as it would for a real booking.
 */
@Service
@Profile("demo")
class DemoDuffelBookingService(
    private val bookingOutboxWriter: BookingOutboxWriter,
) : DuffelBookingService {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun createBooking(
        request: BookingRequest,
        idempotencyKey: String,
        orderId: String?,
    ): BookingResult {
        val fakeOrderId = "ord_demo_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val bookingRef = "DEMO" + Random.nextInt(100_000, 999_999)
        val emails = request.passengers.map { it.email }.distinct()

        log.warn(
            "demo profile: synthesising a confirmed booking (order {}, ref {}) for offer {} -- Duffel was NOT called",
            fakeOrderId,
            bookingRef,
            request.offerId,
        )

        val result =
            BookingResult(
                orderId = fakeOrderId,
                bookingReference = bookingRef,
                status = "confirmed",
                ticketingStatus = "issued",
                totalAmount = Money(amount = 0.0, currency = "EUR"),
                eTicketNumbers = request.passengers.map { "ETK" + Random.nextLong(1_000_000_000L, 9_999_999_999L) },
                passengers =
                    request.passengers.mapIndexed { index, passenger ->
                        BookedPassenger(
                            id = "pas_demo_$index",
                            givenName = passenger.givenName,
                            familyName = passenger.familyName,
                        )
                    },
                emails = emails,
            )

        bookingOutboxWriter.enqueue(
            eventType = "BOOKING_CONFIRMED",
            orderId = result.orderId,
            payload =
                tools.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(
                    BookingEvent(
                        orderId = result.orderId,
                        bookingReference = result.bookingReference,
                        emails = result.emails,
                    ),
                ),
        )

        return result
    }
}
