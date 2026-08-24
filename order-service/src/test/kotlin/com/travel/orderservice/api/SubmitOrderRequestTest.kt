package com.travel.orderservice.api

import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class SubmitOrderRequestTest {
    private fun request() =
        SubmitOrderRequest(
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
    fun `toCommand carries the header-supplied idempotency key and every request field through unchanged`() {
        val req = request()

        val command = req.toCommand("idem-1")

        assertEquals("idem-1", command.idempotencyKey)
        assertEquals(req.offerId, command.offerId)
        assertEquals(req.passengers, command.passengers)
        assertEquals(req.contact, command.contact)
        assertEquals(req.paymentType, command.paymentType)
    }
}
