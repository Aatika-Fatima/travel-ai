package com.travel.payment.internal.gateway.razorpay

import com.razorpay.RazorpayClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// Same pattern as duffel's DuffelClientConfig -- RazorPayGatewayImpl needs a
// RazorpayClient bean, and nothing in this module previously constructed
// one (RazorpayProperties was declared but never registered either).
@Configuration
@EnableConfigurationProperties(RazorpayProperties::class)
class RazorpayClientConfig {
    @Bean
    fun razorpayClient(properties: RazorpayProperties): RazorpayClient =
        RazorpayClient(properties.apiKey, properties.apiSecret)
}
