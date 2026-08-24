package com.travel.orderservice.internal.controller

import com.travel.orderservice.api.OrderStatus
import com.travel.orderservice.api.OrderView
import com.travel.orderservice.api.SubmitOrderRequest
import com.travel.orderservice.api.toCommand
import com.travel.orderservice.internal.service.OrderService
import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.uuid.Uuid

class OrderControllerTest {
    private val orderService: OrderService = mock()
    private val controller = OrderController(orderService)

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
        )

    private fun view(justCreated: Boolean) =
        OrderView(
            orderId = Uuid.random(),
            status = OrderStatus.PENDING_SUBMISSION,
            offerId = "off_1",
            duffelOrderId = null,
            bookingReference = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            justCreated = justCreated,
        )

    @Test
    fun `submit returns 201 when the order was just created`() {
        val created = view(justCreated = true)
        whenever(orderService.submit(request().toCommand("idem-1"))).thenReturn(created)

        val response = controller.submit("idem-1", request())

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(created, response.body)
    }

    @Test
    fun `submit returns 200 when the order already existed`() {
        val existing = view(justCreated = false)
        whenever(orderService.submit(request().toCommand("idem-1"))).thenReturn(existing)

        val response = controller.submit("idem-1", request())

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(existing, response.body)
    }

    @Test
    fun `get returns 200 with the order view for a known id`() {
        val found = view(justCreated = false)
        whenever(orderService.findById(found.orderId)).thenReturn(found)

        val response = controller.get(found.orderId)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(found, response.body)
    }

    @Test
    fun `get returns 404 for an unknown id`() {
        val id = Uuid.random()
        whenever(orderService.findById(id)).thenReturn(null)

        val response = controller.get(id)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `getByIdempotencyKey returns 200 with the order view for a known key`() {
        val found = view(justCreated = false)
        whenever(orderService.findByIdempotencyKey("idem-1")).thenReturn(found)

        val response = controller.getByIdempotencyKey("idem-1")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(found, response.body)
    }

    @Test
    fun `getByIdempotencyKey returns 404 for an unknown key`() {
        whenever(orderService.findByIdempotencyKey("unknown")).thenReturn(null)

        val response = controller.getByIdempotencyKey("unknown")

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
}
