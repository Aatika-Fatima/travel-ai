package com.travel.payment.internal.gateway.razorpay

import com.razorpay.Order
import com.razorpay.RazorpayClient
import org.json.JSONObject
import org.springframework.stereotype.Component

@Component
class RazorPayGatewayImpl(private val client: RazorpayClient,
                          private val properties: RazorpayProperties): RazorPayGateway {
    override fun createOrder(
        amountPaise: Long,
        currency: String,
        idempotencyKey: String
    ): RazorpayOrderResult {
    val request = JSONObject().put("amount", amountPaise).put("currency", currency)
        .put("receipt", idempotencyKey)
        .put("payment_capture",1)
        val order: Order = client.orders.create(request)
        return RazorpayOrderResult(order["id"] as String, order["status"] as String)
    }

    override fun capture(
        razorpayPaymentId: String,
        amountPaise: Long,
        currency: String
    ): RazorpayCaptureResult {
        TODO("Not yet implemented")
    }

    override fun verifyCheckoutSignature(
        orderId: String,
        paymentId: String,
        signature: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun verifyWebhookSignature(rawBody: String, signature: String): Boolean {
        TODO("Not yet implemented")
    }

}