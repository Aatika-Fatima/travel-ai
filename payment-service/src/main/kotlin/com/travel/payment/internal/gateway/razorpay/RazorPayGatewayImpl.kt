package com.travel.payment.internal.gateway.razorpay

import com.razorpay.Order
import com.razorpay.RazorpayClient
import com.razorpay.RazorpayException
import com.razorpay.Utils
import org.hibernate.validator.internal.util.logging.LoggerFactory
import org.jboss.logging.Logger
import org.json.JSONObject
import org.springframework.stereotype.Component

@Component
class RazorPayGatewayImpl(private val client: RazorpayClient,
                          private val properties: RazorpayProperties): RazorpayGateway {
    val log: Logger = Logger.getLogger(RazorPayGatewayImpl::class.java)
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
       val attributes = JSONObject().apply {
           put("razorpay_order_id", orderId)
           put("razorpay_payment_id", paymentId)
           put("razorpay_signature", signature)
       }

        return try {
            // SDK computes HMAC_SHA256(orderId + "|" + paymentId, key_secret) and
            // compares it to `signature` -- this proves the callback really
            // came from Razorpay. It does NOT prove the money was captured;
            // only the webhook in P7 does that.
            Utils.verifyPaymentSignature(attributes, properties.apiSecret)
        } catch (e: RazorpayException) {
            log.error(e.message, e)
            false
        }
       }

    override fun verifyWebhookSignature(rawBody: String, signature: String): Boolean {
        return try {
            // HMAC_SHA256(rawBody, webhook_secret) -- a DIFFERENT secret from
            // apiSecret above. Verifying against the raw, unparsed body is
            // deliberate: any re-serialization (even key reordering) would
            // change the bytes the signature was computed over and break this.
            Utils.verifyWebhookSignature(rawBody, signature, properties.webhookSecret)
        } catch (e: RazorpayException) {
            log.error(e.message, e)
            false
        }
    }
}

