package com.travel.bookingservice.internal.controller

import com.travel.bookingservice.internal.service.BookingService
import com.travel.bookingservice.internal.service.BookingView
import com.travel.bookingservice.internal.persistence.BookingStatus
import com.travel.common.model.BookingRequest
import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.uuid.Uuid

class BookingControllerTest {
    private val bookingService: BookingService = mock()
    private val controller = BookingController(bookingService)

    private fun request() =
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
        )

    private fun view(bookingId: Uuid = Uuid.random(), justCreated: Boolean) =
        BookingView(
            bookingId = bookingId,
            status = BookingStatus.INITIATED,
            offerId = "off_1",
            failureReason = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            justCreated = justCreated,
        )

    @Test
    fun `submit returns 201 when the booking was just created`() {
        val created = view(justCreated = true)
        whenever(bookingService.submit("bk-1", request())).thenReturn(created)

        val response = controller.submit("bk-1", request())

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(created, response.body)
    }

    @Test
    fun `submit returns 200 when the booking already existed`() {
        val existing = view(justCreated = false)
        whenever(bookingService.submit("bk-1", request())).thenReturn(existing)

        val response = controller.submit("bk-1", request())

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(existing, response.body)
    }

    @Test
    fun `get returns 200 with the booking view`() {
        val id = Uuid.random()
        val found = view(bookingId = id, justCreated = false)
        whenever(bookingService.get(id)).thenReturn(found)

        val response = controller.get(id)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(found, response.body)
        verify(bookingService).get(id)
    }

    @Test
    fun `cancel returns 200 with the cancelled booking view`() {
        val id = Uuid.random()
        val cancelled = view(bookingId = id, justCreated = true)
        whenever(bookingService.cancel(id)).thenReturn(cancelled)

        val response = controller.cancel(id)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(cancelled, response.body)
        verify(bookingService).cancel(id)
    }
}
