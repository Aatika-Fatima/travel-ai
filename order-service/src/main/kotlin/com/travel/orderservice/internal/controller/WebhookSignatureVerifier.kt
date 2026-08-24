package com.travel.orderservice.internal.controller

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Duffel signs webhooks as "Duffel-Signature: t=<timestamp>,v1=<hex hmac>"
// (HMAC-SHA256 over "$timestamp.$rawBody"). Never trust an unverified body --
// see DuffelWebhookController, the only caller.
fun verify(signatureHeader: String, rawBody: String, secret: String): Boolean {
    val (timestamp, signature) = parseSignatureHeader(signatureHeader) ?: return false
    val expected = hmacSha256("$timestamp.$rawBody", secret)
    // hexDecode never throws (see below) -- an odd-length or non-hex
    // signature just decodes to bytes that won't match, which isEqual
    // already handles without a separate catch branch.
    val actual = hexDecode(signature)
    // constant-time compare -- a naive == would leak timing information an
    // attacker could use to forge a valid signature byte by byte
    return MessageDigest.isEqual(expected, actual)
}

private fun parseSignatureHeader(header: String): Pair<String, String>? {
    val parts =
        header.split(",").mapNotNull { part ->
            val kv = part.split("=", limit = 2)
            if (kv.size == 2) kv[0].trim() to kv[1].trim() else null
        }.toMap()
    val timestamp = parts["t"] ?: return null
    val signature = parts["v1"] ?: return null
    return timestamp to signature
}

private fun hmacSha256(data: String, secret: String): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
    return mac.doFinal(data.toByteArray())
}

private fun hexDecode(hex: String): ByteArray =
    ByteArray(hex.length / 2) { i ->
        ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte()
    }
