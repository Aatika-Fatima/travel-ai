package com.travel.payment.internal.gateway.razorpay

import com.razorpay.Order
import com.razorpay.OrderClient
import com.razorpay.RazorpayClient
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RazorPayGatewayImplTest {
    private fun properties(apiSecret: String = "key_secret", webhookSecret: String = "webhook_secret") =
        RazorpayProperties(apiKey = "key_id", apiSecret = apiSecret, webhookSecret = webhookSecret)

    private fun hmacSha256Hex(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `createOrder sends amount, currency, receipt and auto-capture, and maps the Razorpay response`() {
        val orderClient = mock<OrderClient>()
        val client = mock<RazorpayClient>()
        client.orders = orderClient
        val captor = argumentCaptor<JSONObject>()
        whenever(orderClient.create(captor.capture()))
            .thenReturn(Order(JSONObject().put("id", "order_123").put("status", "created")))
        val gateway = RazorPayGatewayImpl(client, properties())

        val result = gateway.createOrder(50_000, "INR", "booking:abc")

        assertEquals("order_123", result.razorpayOrderId)
        assertEquals("created", result.status)
        assertEquals(50_000L, captor.firstValue.getLong("amount"))
        assertEquals("INR", captor.firstValue.getString("currency"))
        assertEquals("booking:abc", captor.firstValue.getString("receipt"))
        assertEquals(1, captor.firstValue.getInt("payment_capture"))
    }

    @Test
    fun `capture is not yet implemented`() {
        val gateway = RazorPayGatewayImpl(mock(), properties())

        assertFailsWith<NotImplementedError> { gateway.capture("pay_1", 1_000, "INR") }
    }

    @Test
    fun `verifyCheckoutSignature accepts a signature that actually matches`() {
        val gateway = RazorPayGatewayImpl(mock(), properties(apiSecret = "test_key_secret"))
        val signature = hmacSha256Hex("order_1|pay_1", "test_key_secret")

        assertTrue(gateway.verifyCheckoutSignature("order_1", "pay_1", signature))
    }

    @Test
    fun `verifyCheckoutSignature rejects a signature that does not match`() {
        val gateway = RazorPayGatewayImpl(mock(), properties(apiSecret = "test_key_secret"))

        assertFalse(gateway.verifyCheckoutSignature("order_1", "pay_1", "not-the-real-signature"))
    }

    @Test
    fun `verifyWebhookSignature accepts a signature computed over the raw body with the webhook secret`() {
        val gateway = RazorPayGatewayImpl(mock(), properties(webhookSecret = "test_webhook_secret"))
        val rawBody = """{"event":"payment.captured"}"""
        val signature = hmacSha256Hex(rawBody, "test_webhook_secret")

        assertTrue(gateway.verifyWebhookSignature(rawBody, signature))
    }

    @Test
    fun `verifyWebhookSignature rejects a signature that does not match`() {
        val gateway = RazorPayGatewayImpl(mock(), properties(webhookSecret = "test_webhook_secret"))

        assertFalse(gateway.verifyWebhookSignature("{}", "not-the-real-signature"))
    }

    @Test
    fun `verifyWebhookSignature rejects the wrong secret even with a well-formed signature`() {
        val gateway = RazorPayGatewayImpl(mock(), properties(webhookSecret = "test_webhook_secret"))
        val rawBody = """{"event":"payment.captured"}"""
        val signatureUnderAnotherSecret = hmacSha256Hex(rawBody, "a-different-secret")

        assertFalse(gateway.verifyWebhookSignature(rawBody, signatureUnderAnotherSecret))
    }

    @Test
    fun `verifyCheckoutSignature is false, not thrown, when the SDK itself can't compute a signature`() {
        // An empty secret can't seed HmacSHA256's SecretKeySpec -- the SDK
        // wraps that as RazorpayException. This is a real failure mode (a
        // misconfigured deployment), and it must fail closed, not crash the
        // checkout-confirm endpoint.
        val gateway = RazorPayGatewayImpl(mock(), properties(apiSecret = ""))

        assertFalse(gateway.verifyCheckoutSignature("order_1", "pay_1", "any-signature"))
    }

    @Test
    fun `verifyWebhookSignature is false, not thrown, when the SDK itself can't compute a signature`() {
        val gateway = RazorPayGatewayImpl(mock(), properties(webhookSecret = ""))

        assertFalse(gateway.verifyWebhookSignature("{}", "any-signature"))
    }
}
