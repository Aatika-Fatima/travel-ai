package com.travel.payment.internal.gateway.razorpay

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RazorpayGatewayTypesTest {
    @Test
    fun `RazorpayCaptureResult exposes the mapped payment id and status`() {
        val result = RazorpayCaptureResult("pay_1", "captured")

        assertEquals("pay_1", result.razorpayPaymentId)
        assertEquals("captured", result.status)
        assertEquals(result, result.copy())
    }
}
