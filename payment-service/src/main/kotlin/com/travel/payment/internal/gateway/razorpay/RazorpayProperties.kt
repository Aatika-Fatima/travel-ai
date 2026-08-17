package com.travel.payment.internal.gateway.razorpay

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "razorpay")
class RazorpayProperties(keyId: String,keySecret: String,webhookSecret: String)