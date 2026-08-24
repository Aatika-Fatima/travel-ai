package com.travel.payment.internal.controller

import com.travel.payment.internal.gateway.razorpay.RazorpayGateway
import com.travel.payment.internal.repository.PaymentTransactionalOps
import com.travel.payment.internal.repository.WebhookOutcome
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class RazorpayWebhookControllerTest {
    private val gateway: RazorpayGateway = mock()
    private val transactionalOps: PaymentTransactionalOps = mock()
    private val controller = RazorpayWebhookController(gateway, transactionalOps)

    @Test
    fun `handle returns 400 and never processes the event when the signature is invalid`() {
        whenever(gateway.verifyWebhookSignature(any(), any())).thenReturn(false)

        val response = controller.handle("{}", "bad-sig")

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        verify(transactionalOps, never()).processWebhookOnce(any(), any(), any())
    }

    @Test
    fun `handle uses the payment id as the dedup key for a payment event`() {
        whenever(gateway.verifyWebhookSignature(any(), any())).thenReturn(true)
        whenever(transactionalOps.processWebhookOnce(any(), any(), any())).thenReturn(WebhookOutcome.PROCESSED)
        val body = """{"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_1"}}}}"""

        val response = controller.handle(body, "sig")

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(transactionalOps).processWebhookOnce(eq("payment.captured"), eq("pay_1"), any())
    }

    @Test
    fun `handle uses the refund id, not the payment id, as the dedup key for a refund event`() {
        whenever(gateway.verifyWebhookSignature(any(), any())).thenReturn(true)
        whenever(transactionalOps.processWebhookOnce(any(), any(), any())).thenReturn(WebhookOutcome.PROCESSED)
        val body =
            """
            {"event":"refund.processed","payload":{
                "payment":{"entity":{"id":"pay_1","order_id":"order_1"}},
                "refund":{"entity":{"id":"rfnd_1","status":"processed"}}
            }}
            """.trimIndent()

        controller.handle(body, "sig")

        verify(transactionalOps).processWebhookOnce(eq("refund.processed"), eq("rfnd_1"), any())
    }

    @Test
    fun `handle falls back to event name plus timestamp for events with no payment or refund entity`() {
        whenever(gateway.verifyWebhookSignature(any(), any())).thenReturn(true)
        whenever(transactionalOps.processWebhookOnce(any(), any(), any())).thenReturn(WebhookOutcome.IGNORED_EVENT)
        val body = """{"event":"payment.downtime.started","created_at":1700000000,"payload":{}}"""

        val response = controller.handle(body, "sig")

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(transactionalOps).processWebhookOnce(
            eq("payment.downtime.started"),
            eq("payment.downtime.started:1700000000"),
            any(),
        )
    }

    @Test
    fun `handle returns 200 for a duplicate delivery`() {
        whenever(gateway.verifyWebhookSignature(any(), any())).thenReturn(true)
        whenever(transactionalOps.processWebhookOnce(any(), any(), any())).thenReturn(WebhookOutcome.DUPLICATE)
        val body = """{"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_1"}}}}"""

        val response = controller.handle(body, "sig")

        assertEquals(HttpStatus.OK, response.statusCode)
    }
}
