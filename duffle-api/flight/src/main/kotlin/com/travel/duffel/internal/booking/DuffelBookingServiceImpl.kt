package com.travel.duffel.internal.booking

import com.travel.common.events.BookingEvent
import com.travel.common.exception.BookingException
import com.travel.common.exception.BookingFailureReason
import com.travel.common.model.BookedPassenger
import com.travel.common.model.BookingRequest
import com.travel.common.model.BookingResult
import com.travel.common.model.Money
import com.travel.common.model.PassengerDetails
import com.travel.duffel.api.booking.DuffelBookingService
import com.travel.duffel.internal.client.DuffelOrderClient
import com.travel.duffel.api.dto.request.DuffelIdentityDocumentRequest
import com.travel.duffel.api.dto.request.DuffelOrderPassengerRequest
import com.travel.duffel.api.dto.request.DuffelOrderRequestPayload
import com.travel.duffel.api.dto.request.DuffelPaymentRequest
import com.travel.duffel.api.dto.response.DuffelOffer
import com.travel.duffel.api.dto.response.DuffelOrderData
import com.travel.notification.api.outbox.BookingOutboxWriter
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.util.UUID

@Service
class DuffelBookingServiceImpl(
    private val duffelOrderClient: DuffelOrderClient,
    private val bookingOutboxWriter: BookingOutboxWriter,
) : DuffelBookingService {
    override fun createBooking(request: BookingRequest): BookingResult {
        val offer = duffelOrderClient.fetchOffer(request.offerId)
        assertNotExpired(offer)
        val offerPassengerIds = resolveOfferPassengerIds(request, offer)

        val payload = toOrderPayload(request, offer, offerPassengerIds)
        val idempotencyKey = UUID.randomUUID().toString()
        val order = duffelOrderClient.createOrder(payload, idempotencyKey)
        val emails = request.passengers.map { it.email }.distinct()
        val result = toBookingResult(order, emails)
        enqueueOutboxEvent(result)
        return result
    }

    private fun enqueueOutboxEvent(result: BookingResult) {
        bookingOutboxWriter.enqueue(
            eventType = "BOOKING_CONFIRMED",
            orderId = result.orderId,
            payload = jacksonObjectMapper().writeValueAsString(
                BookingEvent(
                    orderId = result.orderId,
                    bookingReference = result.bookingReference, emails = result.emails
                )
            ),
        )
    }

    private fun assertNotExpired(offer: DuffelOffer) {
        val expiresAt = offer.expiresAt ?: return
        val expiry = runCatching { Instant.parse(expiresAt) }.getOrNull() ?: return
        if (expiry.isBefore(Instant.now())) {
            throw BookingException(
                "This offer has expired. Please search again.",
                reason = BookingFailureReason.OFFER_EXPIRED,
            )
        }
    }

    // The offer never exposes its Duffel-internal passenger ids to callers (they aren't part of
    // the public search response), so they're resolved here from the freshly-fetched offer instead
    // of being supplied by the client - paired positionally with `request.passengers`.
    private fun resolveOfferPassengerIds(
        request: BookingRequest,
        offer: DuffelOffer,
    ): List<String> {
        val offerPassengerIds = offer.passengers?.mapNotNull { it.id } ?: emptyList()
        if (offerPassengerIds.size != request.passengers.size) {
            throw BookingException(
                "Passenger count does not match the offer",
                reason = BookingFailureReason.VALIDATION_FAILED,
                fieldErrors =
                    mapOf(
                        "passengers" to
                            "expected ${offerPassengerIds.size} passenger(s) for this offer, got ${request.passengers.size}",
                    ),
            )
        }
        return offerPassengerIds
    }

    private fun toOrderPayload(
        request: BookingRequest,
        offer: DuffelOffer,
        offerPassengerIds: List<String>,
    ): DuffelOrderRequestPayload =
        DuffelOrderRequestPayload(
            selectedOffers = listOf(offer.id),
            passengers =
                request.passengers.zip(offerPassengerIds).map { (passenger, offerPassengerId) ->
                    toPassengerRequest(passenger, offerPassengerId)
                },
            payments =
                listOf(
                    DuffelPaymentRequest(
                        type = request.paymentType,
                        currency = offer.totalCurrency,
                        amount = offer.totalAmount,
                    ),
                ),
        )

    private fun toPassengerRequest(
        passenger: PassengerDetails,
        offerPassengerId: String,
    ): DuffelOrderPassengerRequest =
        DuffelOrderPassengerRequest(
            id = offerPassengerId,
            title = passenger.title,
            givenName = passenger.givenName,
            familyName = passenger.familyName,
            bornOn = passenger.dateOfBirth.toString(),
            gender = passenger.gender,
            email = passenger.email,
            phoneNumber = passenger.phoneNumber,
            identityDocuments =
                passenger.documents.ifEmpty { null }?.map { document ->
                    DuffelIdentityDocumentRequest(
                        type = document.type,
                        uniqueIdentifier = document.uniqueIdentifier,
                        expiresOn = document.expiresOn.toString(),
                        issuingCountryCode = document.issuingCountryCode,
                    )
                },
        )

    private fun toBookingResult(order: DuffelOrderData, emails: List<String>): BookingResult =
        BookingResult(
            orderId = order.id,
            bookingReference = order.bookingReference.orEmpty(),
            status = order.status.orEmpty(),
            ticketingStatus = order.status,
            totalAmount = Money(amount = order.totalAmount?.toDoubleOrNull() ?: 0.0, currency = order.totalCurrency.orEmpty()),
            eTicketNumbers =
                order.documents
                    ?.filter { it.type == "electronic_ticket" }
                    ?.mapNotNull { it.uniqueIdentifier }
                    ?: emptyList(),
            passengers =
                order.passengers?.mapNotNull { passenger ->
                    val id = passenger.id ?: return@mapNotNull null
                    BookedPassenger(
                        id = id,
                        givenName = passenger.givenName.orEmpty(),
                        familyName = passenger.familyName.orEmpty(),
                    )
                } ?: emptyList(), emails = emails,
        )
}