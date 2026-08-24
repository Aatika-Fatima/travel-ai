package com.travel.bookingservice.internal.kafka

import com.travel.bookingservice.internal.persistence.BookingStatus
import com.travel.bookingservice.internal.service.BookingStatusEvent
import com.travel.bookingservice.internal.service.BookingTransactionalOps
import com.travel.bookingservice.internal.service.IllegalStateTransitionException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.kafka.support.Acknowledgment
import tools.jackson.databind.ObjectMapper
import kotlin.uuid.Uuid

class OrderEventConsumerTest {
    private val transactionalOps: BookingTransactionalOps = mock()
    private val objectMapper: ObjectMapper = mock()
    private val ack: Acknowledgment = mock()
    private val consumer = OrderEventConsumer(transactionalOps, objectMapper)

    private fun stub(envelope: OrderEventEnvelope, payload: String = "payload") {
        whenever(objectMapper.readValue(payload, OrderEventEnvelope::class.java)).thenReturn(envelope)
    }

    @Test
    fun `OrderAwaitingConfirmation maps to PROVIDER_RESERVED with no failure reason`() {
        val bookingId = Uuid.random()
        stub(OrderEventEnvelope(bookingId, "OrderAwaitingConfirmation"))

        consumer.onOrderEvent("payload", ack)

        verify(transactionalOps).advance(bookingId, BookingStatusEvent.PROVIDER_RESERVED, null)
        verify(ack).acknowledge()
    }

    @Test
    fun `OrderConfirmed maps to PROVIDER_CONFIRMED`() {
        val bookingId = Uuid.random()
        stub(OrderEventEnvelope(bookingId, "OrderConfirmed"))

        consumer.onOrderEvent("payload", ack)

        verify(transactionalOps).advance(bookingId, BookingStatusEvent.PROVIDER_CONFIRMED, null)
        verify(ack).acknowledge()
    }

    @Test
    fun `OrderFailed maps to PROVIDER_FAILED and carries the failure reason through`() {
        val bookingId = Uuid.random()
        stub(OrderEventEnvelope(bookingId, "OrderFailed", failureReason = "card_declined"))

        consumer.onOrderEvent("payload", ack)

        verify(transactionalOps).advance(bookingId, BookingStatusEvent.PROVIDER_FAILED, "card_declined")
        verify(ack).acknowledge()
    }

    @Test
    fun `OrderPaymentFailed also maps to PROVIDER_FAILED`() {
        val bookingId = Uuid.random()
        stub(OrderEventEnvelope(bookingId, "OrderPaymentFailed", failureReason = "insufficient_funds"))

        consumer.onOrderEvent("payload", ack)

        verify(transactionalOps).advance(bookingId, BookingStatusEvent.PROVIDER_FAILED, "insufficient_funds")
    }

    @Test
    fun `OrderCancelled maps to CANCEL_CONFIRMED`() {
        val bookingId = Uuid.random()
        stub(OrderEventEnvelope(bookingId, "OrderCancelled"))

        consumer.onOrderEvent("payload", ack)

        verify(transactionalOps).advance(bookingId, BookingStatusEvent.CANCEL_CONFIRMED, null)
        verify(ack).acknowledge()
    }

    @Test
    fun `an unrecognized event type never calls advance but still acknowledges`() {
        val bookingId = Uuid.random()
        stub(OrderEventEnvelope(bookingId, "OrderTicketed"))

        consumer.onOrderEvent("payload", ack)

        verifyNoInteractions(transactionalOps)
        verify(ack).acknowledge()
    }

    @Test
    fun `a stale or redelivered transition is swallowed and still acknowledged`() {
        val bookingId = Uuid.random()
        stub(OrderEventEnvelope(bookingId, "OrderConfirmed"))
        whenever(transactionalOps.advance(bookingId, BookingStatusEvent.PROVIDER_CONFIRMED, null))
            .thenThrow(IllegalStateTransitionException(BookingStatus.CANCELLED, BookingStatusEvent.PROVIDER_CONFIRMED))

        consumer.onOrderEvent("payload", ack)

        verify(ack).acknowledge()
    }

    @Test
    fun `a message this consumer can't even parse is skipped and acknowledged, not retried forever`() {
        whenever(objectMapper.readValue("bad-payload", OrderEventEnvelope::class.java))
            .thenThrow(RuntimeException("missing required field"))

        consumer.onOrderEvent("bad-payload", ack)

        verifyNoInteractions(transactionalOps)
        verify(ack).acknowledge()
    }
}
