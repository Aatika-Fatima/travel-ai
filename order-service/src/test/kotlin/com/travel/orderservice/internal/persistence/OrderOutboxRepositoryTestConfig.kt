package com.travel.orderservice.internal.persistence

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// order-service has no @SpringBootApplication of its own (it's a library
// assembled by app), so @DataJpaTest needs an explicit bootstrap config to
// anchor entity/repository scanning -- same shape as
// AirportRepositoryTestConfig in duffle-api/flight.
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["com.travel.orderservice.internal.persistence"])
@EnableJpaRepositories(basePackages = ["com.travel.orderservice.internal.persistence"])
class OrderOutboxRepositoryTestConfig
