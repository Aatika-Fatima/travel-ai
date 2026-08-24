package com.travel.orderservice.internal.controller

import org.junit.jupiter.api.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebhookSignatureVerifierTest {
    private val secret = "test-secret"
    private val body = """{"type":"order.created"}"""

    private fun sign(timestamp: String, rawBody: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        return mac.doFinal("$timestamp.$rawBody".toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `verify accepts a signature computed over the raw body with the shared secret`() {
        val timestamp = "1700000000"
        val signature = sign(timestamp, body, secret)

        assertTrue(verify("t=$timestamp,v1=$signature", body, secret))
    }

    @Test
    fun `verify tolerates an unrecognized segment alongside valid t and v1 components`() {
        val timestamp = "1700000000"
        val signature = sign(timestamp, body, secret)

        assertTrue(verify("garbage,t=$timestamp,v1=$signature", body, secret))
    }

    @Test
    fun `verify rejects a signature computed with the wrong secret`() {
        val timestamp = "1700000000"
        val signature = sign(timestamp, body, "wrong-secret")

        assertFalse(verify("t=$timestamp,v1=$signature", body, secret))
    }

    @Test
    fun `verify rejects a tampered body`() {
        val timestamp = "1700000000"
        val signature = sign(timestamp, body, secret)

        assertFalse(verify("t=$timestamp,v1=$signature", "$body-tampered", secret))
    }

    @Test
    fun `verify rejects a header missing the v1 component`() {
        assertFalse(verify("t=1700000000", body, secret))
    }

    @Test
    fun `verify rejects a header missing the t component`() {
        val signature = sign("1700000000", body, secret)

        assertFalse(verify("v1=$signature", body, secret))
    }

    @Test
    fun `verify rejects a non-hex signature`() {
        assertFalse(verify("t=1700000000,v1=not-hex-zz", body, secret))
    }

    @Test
    fun `verify rejects an empty header`() {
        assertFalse(verify("", body, secret))
    }
}
