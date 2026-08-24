package com.travel.orderservice.internal.service

import com.travel.common.exception.BookingException
import com.travel.common.exception.BookingFailureReason
import com.travel.common.exception.ExternalApiException
import com.travel.common.model.BookingResult
import com.travel.common.model.Money
import com.travel.orderservice.api.OrderStatusEvent
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DuffelOutcomeTest {
    private fun bookingResult() =
        BookingResult(
            orderId = "duf_order_1",
            bookingReference = "ABC123",
            status = "confirmed",
            totalAmount = Money(amount = 199.99, currency = "USD"),
            emails = listOf("alex@example.com"),
        )

    @Test
    fun `confirmed always maps to DUFFEL_2XX_FULL`() {
        assertEquals(OrderStatusEvent.DUFFEL_2XX_FULL, DuffelOutcome.confirmed(bookingResult()))
    }

    @Test
    fun `a 202 from Duffel maps to DUFFEL_PENDING -- never retried`() {
        val ex = ExternalApiException("accepted", statusCode = 202)
        assertEquals(OrderStatusEvent.DUFFEL_PENDING, DuffelOutcome.from(ex))
    }

    @Test
    fun `an ambiguous 500 from Duffel also maps to DUFFEL_PENDING`() {
        val ex = ExternalApiException("server error", statusCode = 500)
        assertEquals(OrderStatusEvent.DUFFEL_PENDING, DuffelOutcome.from(ex))
    }

    @Test
    fun `a payment decline maps to DUFFEL_PAYMENT_DECLINED`() {
        val ex = BookingException("declined", reason = BookingFailureReason.PAYMENT_DECLINED)
        assertEquals(OrderStatusEvent.DUFFEL_PAYMENT_DECLINED, DuffelOutcome.from(ex))
    }

    @Test
    fun `a validation failure maps to DUFFEL_VALIDATION_FAILED`() {
        val ex = BookingException("rejected", reason = BookingFailureReason.VALIDATION_FAILED)
        assertEquals(OrderStatusEvent.DUFFEL_VALIDATION_FAILED, DuffelOutcome.from(ex))
    }

    @Test
    fun `a retryable 503 is rethrown unchanged, not mapped to an event`() {
        val ex = ExternalApiException("unavailable", statusCode = 503, retryable = true)
        val thrown = assertFailsWith<ExternalApiException> { DuffelOutcome.from(ex) }
        assertSame(ex, thrown)
    }

    @Test
    fun `an unrecognized exception is rethrown unchanged`() {
        val ex = IllegalStateException("unexpected")
        val thrown = assertFailsWith<IllegalStateException> { DuffelOutcome.from(ex) }
        assertSame(ex, thrown)
    }

    @Test
    fun `an ExternalApiException with no status code at all is rethrown unchanged`() {
        val ex = ExternalApiException("unknown failure", statusCode = null)
        val thrown = assertFailsWith<ExternalApiException> { DuffelOutcome.from(ex) }
        assertSame(ex, thrown)
    }

    @Test
    fun `a BookingException with an unrelated reason is rethrown unchanged, not mapped to an event`() {
        val ex = BookingException("offer expired", reason = BookingFailureReason.OFFER_EXPIRED)
        val thrown = assertFailsWith<BookingException> { DuffelOutcome.from(ex) }
        assertSame(ex, thrown)
    }
}
