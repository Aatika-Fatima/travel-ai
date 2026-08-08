package com.travel.flightsearch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.retry.annotation.EnableRetry

@SpringBootApplication
@EnableRetry
@ComponentScan(basePackages = ["com.travel.flightsearch", "com.travel.duffel"])
class FlightSearchApplication

fun main(args: Array<String>) {
    runApplication<FlightSearchApplication>(*args)
}
