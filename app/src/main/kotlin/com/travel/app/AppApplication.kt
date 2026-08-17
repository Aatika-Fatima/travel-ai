package com.travel.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

// Assembles the search-service, duffel (flight), search-ai, and notification modules into one
// runnable process. No business logic lives here - only wiring, environment config, and the
// cross-module ArchUnit boundary test (see ModuleBoundaryArchTest).
@SpringBootApplication
@EnableRetry
@EnableScheduling
@ComponentScan(
    basePackages = [
        "com.travel.app",
        "com.travel.searchservice",
        "com.travel.duffel",
        "com.travel.searchai",
        "com.travel.notification",
    ],
)
@EntityScan(
    basePackages = [
        "com.travel.common.entity",
        "com.travel.duffel.internal.airport.entity",
        "com.travel.notification.internal.entity",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "com.travel.duffel.internal.airport.repository",
        "com.travel.notification.internal.repository",
    ],
)
@EnableElasticsearchRepositories(basePackages = ["com.travel.duffel.internal.airport.document"])
class AppApplication

fun main(args: Array<String>) {
    runApplication<AppApplication>(*args)
}
