package com.travel.payment.internal.gateway.razorpay

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class RazorpayClientConfigTest {
    @Test
    fun `razorpayClient bean is built from the configured api key and secret`() {
        val properties = RazorpayProperties(apiKey = "key_id", apiSecret = "key_secret", webhookSecret = "webhook_secret")

        val client = RazorpayClientConfig().razorpayClient(properties)

        assertNotNull(client)
    }
}
