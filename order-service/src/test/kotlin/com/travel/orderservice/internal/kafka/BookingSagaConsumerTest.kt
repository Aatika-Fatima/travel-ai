package com.travel.orderservice.internal.kafka

import com.travel.common.model.BookingRequest
import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import com.travel.orderservice.api.SubmitOrderCommand
import com.travel.orderservice.internal.service.OrderMetrics
import com.travel.orderservice.internal.service.OrderService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.kafka.support.Acknowledgment
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import kotlin.test.assertEquals

class BookingSagaConsumerTest {
    private val orderService: OrderService = mock()
    private val metrics: OrderMetrics = mock()
    private val consumer = BookingSagaConsumer(orderService, metrics)
    private val mapper = jacksonObjectMapper()
    private val ack: Acknowledgment = mock()

    private fun payload() =
        BookingCreatedPayload(
            bookingId = kotlin.uuid.Uuid.random().toString(),
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
                ),
        )

    @Test
    fun `onBookingCreated submits the mapped command, reusing booking-service's idempotency key, and acknowledges`() {
        val bookingCreated = payload()
        val json = mapper.writeValueAsString(bookingCreated)

        consumer.onBookingCreated(json, ack)

        val captor = argumentCaptor<SubmitOrderCommand>()
        verify(orderService).submit(captor.capture())
        val command = captor.firstValue
        assertEquals(bookingCreated.idempotencyKey, command.idempotencyKey)
        assertEquals(bookingCreated.offerId, command.offerId)
        assertEquals(bookingCreated.request.passengers, command.passengers)
        assertEquals(bookingCreated.request.contact, command.contact)
        verify(ack).acknowledge()
    }
}
