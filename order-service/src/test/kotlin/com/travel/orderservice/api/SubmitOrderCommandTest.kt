package com.travel.orderservice.api

import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class SubmitOrderCommandTest {
    private fun command() =
        SubmitOrderCommand(
            idempotencyKey = "idem-1",
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
        )

    @Test
    fun `toBookingRequest rebuilds exactly what the Duffel gateway expects, dropping the idempotency key`() {
        val cmd = command()

        val bookingRequest = cmd.toBookingRequest()

        assertEquals(cmd.offerId, bookingRequest.offerId)
        assertEquals(cmd.passengers, bookingRequest.passengers)
        assertEquals(cmd.contact, bookingRequest.contact)
        assertEquals(cmd.paymentType, bookingRequest.paymentType)
    }

    @Test
    fun `paymentType defaults to balance when not supplied`() {
        val cmd =
            SubmitOrderCommand(
                idempotencyKey = "idem-1",
                offerId = "off_1",
                passengers = emptyList(),
                contact = ContactInfo(email = "alex@example.com", phoneNumber = "+10000000000"),
            )

        assertEquals("balance", cmd.paymentType)
    }
}
