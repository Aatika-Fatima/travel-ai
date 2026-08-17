package com.travel.duffel.internal.airport.repository

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// `flight` has no @SpringBootApplication of its own (it's a library), so @DataJpaTest
// needs an explicit bootstrap config to anchor entity/repository scanning.
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["com.travel.common.entity", "com.travel.duffel.internal.airport.entity"])
@EnableJpaRepositories(basePackages = ["com.travel.duffel.internal.airport.repository"])
class AirportRepositoryTestConfig
