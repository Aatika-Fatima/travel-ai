package com.travel.duffel.booking

import com.travel.common.exception.BookingException
import com.travel.common.exception.BookingFailureReason
import com.travel.common.model.BookedPassenger
import com.travel.common.model.BookingRequest
import com.travel.common.model.BookingResult
import com.travel.common.model.Money
import com.travel.common.model.PassengerDetails
import com.travel.duffel.client.DuffelOrderClient
import com.travel.duffel.dto.request.DuffelIdentityDocumentRequest
import com.travel.duffel.dto.request.DuffelOrderPassengerRequest
import com.travel.duffel.dto.request.DuffelOrderRequestPayload
import com.travel.duffel.dto.request.DuffelPaymentRequest
import com.travel.duffel.dto.response.DuffelOffer
import com.travel.duffel.dto.response.DuffelOrderData
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class DuffelBookingServiceImpl(
    private val duffelOrderClient: DuffelOrderClient,
) : DuffelBookingService {
    override fun createBooking(request: BookingRequest): BookingResult {
        val offer = duffelOrderClient.fetchOffer(request.offerId)
        assertNotExpired(offer)
        assertPassengersMatchOffer(request, offer)

        val payload = toOrderPayload(request, offer)
        val idempotencyKey = UUID.randomUUID().toString()
        val order = duffelOrderClient.createOrder(payload, idempotencyKey)

        return toBookingResult(order)
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

    private fun assertPassengersMatchOffer(
        request: BookingRequest,
        offer: DuffelOffer,
    ) {
        val offerPassengerIds = offer.passengers?.mapNotNull { it.id }?.toSet() ?: emptySet()
        val fieldErrors =
            request.passengers
                .mapIndexedNotNull { index, passenger ->
                    if (passenger.offerPassengerId !in offerPassengerIds) {
                        "passengers[$index].offerPassengerId" to "does not match any passenger on this offer"
                    } else {
                        null
                    }
                }.toMap()

        if (fieldErrors.isNotEmpty()) {
            throw BookingException(
                "Passenger details do not match the offer",
                reason = BookingFailureReason.VALIDATION_FAILED,
                fieldErrors = fieldErrors,
            )
        }
    }

    private fun toOrderPayload(
        request: BookingRequest,
        offer: DuffelOffer,
    ): DuffelOrderRequestPayload =
        DuffelOrderRequestPayload(
            selectedOffers = listOf(offer.id),
            passengers = request.passengers.map { toPassengerRequest(it) },
            payments =
                listOf(
                    DuffelPaymentRequest(
                        type = request.paymentType,
                        currency = offer.totalCurrency,
                        amount = offer.totalAmount,
                    ),
                ),
        )

    private fun toPassengerRequest(passenger: PassengerDetails): DuffelOrderPassengerRequest =
        DuffelOrderPassengerRequest(
            id = passenger.offerPassengerId,
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

    private fun toBookingResult(order: DuffelOrderData): BookingResult =
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
                } ?: emptyList(),
        )
}
