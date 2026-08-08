package com.travel.flightsearch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableRetry
@EnableScheduling
@ComponentScan(basePackages = ["com.travel.flightsearch", "com.travel.duffel"])
@EntityScan(basePackages = ["com.travel.common.entity", "com.travel.duffel.airport.entity"])
@EnableJpaRepositories(basePackages = ["com.travel.duffel.airport.repository"])
class FlightSearchApplication

fun main(args: Array<String>) {
    runApplication<FlightSearchApplication>(*args)
}
