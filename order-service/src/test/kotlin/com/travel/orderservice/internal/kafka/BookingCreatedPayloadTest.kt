package com.travel.orderservice.internal.kafka

import com.travel.common.model.BookingRequest
import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class BookingCreatedPayloadTest {
    private val bookingId = Uuid.random()

    private fun payload() =
        BookingCreatedPayload(
            bookingId = bookingId.toString(),
            idempotencyKey = "bk-1",
            offerId = "off_1",
            request =
                BookingRequest(
                    offerId = "off_1",
                    passengers =
                        listOf(
                            PassengerDetails(
                                title = "Mr",
                                givenName = "Alex",
                                familyName = "Doe",
                                dateOfBirth = LocalDate.of(1990, 1, 1),
                                gender = "m",
                                email = "alex@example.com",
                                phoneNumber = "+10000000000",
                            ),
                        ),
                    contact = ContactInfo(email = "alex@example.com", phoneNumber = "+10000000000"),
                    paymentType = "balance",
                ),
        )

    @Test
    fun `toSubmitOrderCommand reuses booking-service's idempotency key, never deriving a new one`() {
        val bookingCreated = payload()

        val command = bookingCreated.toSubmitOrderCommand()

        assertEquals(bookingCreated.idempotencyKey, command.idempotencyKey)
        assertEquals(bookingCreated.offerId, command.offerId)
        assertEquals(bookingCreated.request.passengers, command.passengers)
        assertEquals(bookingCreated.request.contact, command.contact)
        assertEquals(bookingCreated.request.paymentType, command.paymentType)
        // Seeds order-service's own order id from booking-service's booking
        // id -- see SubmitOrderCommand.orderId's doc comment for why.
        assertEquals(bookingId, command.orderId)
    }
}
