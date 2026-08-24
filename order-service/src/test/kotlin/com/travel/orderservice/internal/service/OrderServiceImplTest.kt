package com.travel.orderservice.internal.service

import com.travel.common.exception.ExternalApiException
import com.travel.common.model.BookingResult
import com.travel.common.model.ContactInfo
import com.travel.common.model.Money
import com.travel.common.model.PassengerDetails
import com.travel.duffel.api.booking.DuffelBookingService
import com.travel.orderservice.api.OrderStatus
import com.travel.orderservice.api.OrderStatusEvent
import com.travel.orderservice.api.OrderView
import com.travel.orderservice.api.SubmitOrderCommand
import com.travel.orderservice.internal.outbox.OutboxWriter
import com.travel.orderservice.internal.persistence.OrderEntity
import com.travel.orderservice.internal.persistence.OrderRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock

class OrderServiceImplTest {
    private val orders: OrderRepository = mock()
    private val gateway: DuffelBookingService = mock()
    private val outbox: OutboxWriter = mock()
    // A spy around a real OrderMetrics -- timeDuffelCall() is a generic
    // higher-order function; spying a real instance actually runs the block
    // (so callDuffelAndAdvance's own logic executes normally) while still
    // letting verify() check which counters this service calls, without
    // fighting Mockito's stubbing of a generic function-typed parameter.
    private val metrics: OrderMetrics = spy(OrderMetrics(SimpleMeterRegistry()))
    private val transitions: OrderTransitions = mock()
    private val service = OrderServiceImpl(orders, gateway, outbox, metrics, transitions)

    private fun command(idempotencyKey: String = "idem-1") =
        SubmitOrderCommand(
            idempotencyKey = idempotencyKey,
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

    private fun bookingResult() =
        BookingResult(
            orderId = "duf_order_1",
            bookingReference = "ABC123",
            status = "confirmed",
            totalAmount = Money(amount = 199.99, currency = "USD"),
            emails = listOf("alex@example.com"),
        )

    private fun view(orderId: kotlin.uuid.Uuid) =
        OrderView(
            orderId = orderId,
            status = OrderStatus.CONFIRMED,
            offerId = "off_1",
            duffelOrderId = "duf_order_1",
            bookingReference = "ABC123",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )

    @Test
    fun `submit returns the existing order without calling Duffel when the idempotency key is already known`() {
        val existing = OrderEntity(idempotencyKey = "idem-1", offerId = "off_1")
        whenever(orders.findByIdempotencyKey("idem-1")).thenReturn(existing)

        val result = service.submit(command())

        assertEquals(existing.id, result.orderId)
        assertEquals(false, result.justCreated)
        verify(metrics).duplicatePrevented("unique_constraint")
        verify(gateway, never()).createBooking(any(), any(), any())
        verify(orders, never()).saveAndFlush(any<OrderEntity>())
    }

    @Test
    fun `submit inserts a PENDING row, writes an OrderPending outbox event, and calls Duffel with the order's own id and idempotency key`() {
        whenever(orders.findByIdempotencyKey("idem-1")).thenReturn(null)
        val saved = OrderEntity(idempotencyKey = "idem-1", offerId = "off_1")
        whenever(orders.saveAndFlush(any<OrderEntity>())).thenReturn(saved)
        whenever(gateway.createBooking(any(), eq("idem-1"), eq(saved.id.toString()))).thenReturn(bookingResult())
        whenever(transitions.advance(saved.id, OrderStatusEvent.DUFFEL_2XX_FULL)).thenReturn(view(saved.id))

        val result = service.submit(command())

        assertEquals(saved.id, result.orderId)
        // The whole point of 201-vs-200 in OrderController -- advance()'s
        // own return value has no idea this row was just inserted (it's
        // shared with the webhook controller and the reconciliation sweep),
        // so callDuffelAndAdvance() has to stamp this itself.
        assertEquals(true, result.justCreated)
        val outboxCaptor = argumentCaptor<OutboxEvent>()
        verify(outbox).append(eq(saved.id), outboxCaptor.capture())
        assertEquals("OrderPending", outboxCaptor.firstValue.eventType)
        verify(transitions).advance(saved.id, OrderStatusEvent.DUFFEL_2XX_FULL)
        // Duffel's response data has nowhere else to land -- advance() only
        // ever gets a status event, not a BookingResult.
        assertEquals("duf_order_1", saved.duffelOrderId)
        assertEquals("ABC123", saved.bookingReference)
    }

    @Test
    fun `submit seeds the new order's own id from command-orderId when the command came from the booking-service saga`() {
        val seededId = kotlin.uuid.Uuid.random()
        whenever(orders.findByIdempotencyKey("idem-1")).thenReturn(null)
        whenever(orders.saveAndFlush(any<OrderEntity>())).thenAnswer { it.arguments[0] }
        whenever(gateway.createBooking(any(), any(), any())).thenReturn(bookingResult())
        whenever(transitions.advance(eq(seededId), any())).thenReturn(view(seededId))

        service.submit(command().copy(orderId = seededId))

        val entityCaptor = argumentCaptor<OrderEntity>()
        verify(orders).saveAndFlush(entityCaptor.capture())
        assertEquals(seededId, entityCaptor.firstValue.id)
    }

    @Test
    fun `submit maps a Duffel failure through DuffelOutcome and still advances the state machine`() {
        whenever(orders.findByIdempotencyKey("idem-1")).thenReturn(null)
        val saved = OrderEntity(idempotencyKey = "idem-1", offerId = "off_1")
        whenever(orders.saveAndFlush(any<OrderEntity>())).thenReturn(saved)
        val failure = ExternalApiException("accepted", statusCode = 202)
        whenever(gateway.createBooking(any(), any(), any())).thenThrow(failure)
        whenever(transitions.advance(saved.id, OrderStatusEvent.DUFFEL_PENDING)).thenReturn(view(saved.id))

        val result = service.submit(command())

        verify(transitions).advance(saved.id, OrderStatusEvent.DUFFEL_PENDING)
        assertEquals(true, result.justCreated)
    }

    @Test
    fun `submit recovers from a concurrent insert race by re-reading the winning row, without calling Duffel`() {
        whenever(orders.findByIdempotencyKey("idem-1")).thenReturn(null)
        val winner = OrderEntity(idempotencyKey = "idem-1", offerId = "off_1")
        whenever(orders.saveAndFlush(any<OrderEntity>())).thenThrow(DataIntegrityViolationException("duplicate key"))
        whenever(orders.findByIdempotencyKey("idem-1"))
            .thenReturn(null)
            .thenReturn(winner)

        val result = service.submit(command())

        assertEquals(winner.id, result.orderId)
        assertEquals(false, result.justCreated)
        verify(metrics).duplicatePrevented("unique_constraint_race")
        verify(gateway, never()).createBooking(any(), any(), any())
    }

    @Test
    fun `submit rethrows the original constraint violation if the recovery read still finds nothing`() {
        whenever(orders.findByIdempotencyKey("idem-1")).thenReturn(null)
        val original = DataIntegrityViolationException("duplicate key")
        whenever(orders.saveAndFlush(any<OrderEntity>())).thenThrow(original)

        val thrown = assertFailsWith<DataIntegrityViolationException> { service.submit(command()) }

        assertEquals(original, thrown)
    }

    @Test
    fun `findById returns the view for a known order`() {
        val entity = OrderEntity(idempotencyKey = "idem-1", offerId = "off_1")
        whenever(orders.findById(entity.id)).thenReturn(java.util.Optional.of(entity))

        assertEquals(entity.id, service.findById(entity.id)?.orderId)
    }

    @Test
    fun `findById returns null for an unknown order`() {
        val id = kotlin.uuid.Uuid.random()
        whenever(orders.findById(id)).thenReturn(java.util.Optional.empty())

        assertEquals(null, service.findById(id))
    }

    @Test
    fun `findByIdempotencyKey returns the view for a known key`() {
        val entity = OrderEntity(idempotencyKey = "idem-1", offerId = "off_1")
        whenever(orders.findByIdempotencyKey("idem-1")).thenReturn(entity)

        assertEquals(entity.id, service.findByIdempotencyKey("idem-1")?.orderId)
    }

    @Test
    fun `findByIdempotencyKey returns null for an unknown key`() {
        whenever(orders.findByIdempotencyKey("unknown")).thenReturn(null)

        assertEquals(null, service.findByIdempotencyKey("unknown"))
    }
}
