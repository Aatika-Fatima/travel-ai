package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.outbox.BookingEventOutboxWriter
import com.travel.bookingservice.internal.outbox.BookingEventOutboxWriterImpl
import com.travel.bookingservice.internal.persistence.BookingEventOutboxRepository
import com.travel.bookingservice.internal.persistence.BookingRepository
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import tools.jackson.databind.ObjectMapper

// booking-service has no @SpringBootApplication of its own (it's a library
// assembled by app), so @DataJpaTest needs an explicit bootstrap config to
// anchor entity/repository scanning -- same shape as
// AirportRepositoryTestConfig in duffle-api/flight.
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["com.travel.bookingservice.internal.persistence"])
@EnableJpaRepositories(basePackages = ["com.travel.bookingservice.internal.persistence"])
class BookingConcurrencyTestConfig {
    // BookingTransactionalOps needs one of these injected -- @DataJpaTest
    // doesn't pull in Jackson's own auto-configuration, so it's provided
    // here rather than assumed.
    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper()

    @Bean
    fun bookingOutboxWriter(repository: BookingEventOutboxRepository): BookingEventOutboxWriter = BookingEventOutboxWriterImpl(repository)

    // The real @Transactional-annotated bean, not a hand-built instance --
    // the whole point of this test is proving what Spring's transaction
    // proxy actually does under concurrent load, which a plain `new` skips.
    @Bean
    fun bookingTransactionalOps(
        bookingRepository: BookingRepository,
        outboxWriter: BookingEventOutboxWriter,
        objectMapper: ObjectMapper,
    ) = BookingTransactionalOps(bookingRepository, outboxWriter, objectMapper)
}
