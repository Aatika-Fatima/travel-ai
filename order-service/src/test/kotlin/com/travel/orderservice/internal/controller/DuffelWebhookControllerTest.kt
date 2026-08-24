package com.travel.orderservice.internal.controller

import com.travel.orderservice.api.OrderStatusEvent
import com.travel.orderservice.internal.service.OrderTransitions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpStatus
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class DuffelWebhookControllerTest {
    private val transitions: OrderTransitions = mock()
    private val secret = "test-secret"
    private val controller = DuffelWebhookController(transitions, secret)

    private fun sign(rawBody: String, timestamp: String = "1700000000"): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val hex = mac.doFinal("$timestamp.$rawBody".toByteArray()).joinToString("") { "%02x".format(it) }
        return "t=$timestamp,v1=$hex"
    }

    @Test
    fun `handle returns 401 and never advances the order when the signature is invalid`() {
        val body = """{"type":"order.created"}"""

        val response = controller.handle(body, "t=1700000000,v1=deadbeef")

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        verifyNoInteractions(transitions)
    }

    @Test
    fun `handle acknowledges but ignores any event type other than order-created`() {
        val body = """{"type":"order.cancelled"}"""

        val response = controller.handle(body, sign(body))

        assertEquals(HttpStatus.OK, response.statusCode)
        verifyNoInteractions(transitions)
    }

    @Test
    fun `handle acknowledges a verified body that has no type field at all, without throwing`() {
        val body = "{}"

        val response = controller.handle(body, sign(body))

        assertEquals(HttpStatus.OK, response.statusCode)
        verifyNoInteractions(transitions)
    }

    @Test
    fun `handle advances WEBHOOK_CONFIRMED using metadata-internal_order_id from an order-created event`() {
        val orderId = Uuid.random()
        val body = """{"type":"order.created","data":{"metadata":{"internal_order_id":"$orderId"}}}"""

        val response = controller.handle(body, sign(body))

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(transitions).advance(orderId, OrderStatusEvent.WEBHOOK_CONFIRMED)
    }
}
