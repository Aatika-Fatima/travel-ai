package com.travel.duffel.internal.config.client

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(DuffelProperties::class)
class DuffelClientConfig {
    @Bean
    fun duffelRestClient(properties: DuffelProperties): RestClient {
        val timeout = Duration.ofMillis(properties.timeoutMs)
        val requestFactory =
            ClientHttpRequestFactoryBuilder
                .detect()
                .build(HttpClientSettings.defaults().withTimeouts(timeout, timeout))

        return RestClient
            .builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader("Authorization", "Bearer ${properties.apiKey}")
            .defaultHeader("Duffel-Version", properties.version)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Accept-Encoding", "gzip")
            .build()
    }
}
