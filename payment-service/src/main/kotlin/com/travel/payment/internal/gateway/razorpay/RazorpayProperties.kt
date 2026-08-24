package com.travel.payment.internal.gateway.razorpay

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "razorpay")
class RazorpayProperties(val apiKey: String, val apiSecret: String, val webhookSecret: String)