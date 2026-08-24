package com.travel.app

import com.travel.common.util.KotlinUuidModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.JacksonModule

// Spring Boot's Jackson autoconfiguration picks up any JacksonModule bean
// and registers it on the shared ObjectMapper -- see KotlinUuidModule for
// why one is needed here.
@Configuration
class JacksonConfig {
    @Bean
    fun kotlinUuidModule(): JacksonModule = KotlinUuidModule()
}
