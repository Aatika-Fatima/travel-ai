package com.travel.searchservice.internal.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// Permits the Vite dev server (a different origin during local frontend
// development) to call this API directly.
//
// The production build is served from the same origin as this app, but the
// app sits behind Caddy (TLS terminated there, proxied to plain http on
// 127.0.0.1:8090). Unless Spring is told to trust the X-Forwarded-* headers
// (server.forward-headers-strategy, set in application-aws.yaml), it
// reconstructs the request URL as http://127.0.0.1:8090 and then treats a
// browser's "Origin: https://www.aatikatechstack.com" on a same-origin
// fetch() as a cross-origin request -- CorsFilter then rejects it with
// 403 "Invalid CORS request". Listing the public origins here keeps that
// path working even if the forwarded-headers config is ever lost.
@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/api/**")
            .allowedOriginPatterns(
                "http://localhost:*",
                "https://aatikatechstack.com",
                "https://www.aatikatechstack.com",
            )
            .allowedMethods("GET", "POST")
    }
}
