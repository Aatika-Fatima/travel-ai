package com.travel.orderservice.internal.service

import com.travel.common.model.BookedPassenger
import com.travel.common.model.BookingRequest
import com.travel.common.model.BookingResult
import com.travel.common.model.Money
import com.travel.duffel.api.booking.DuffelBookingService
import com.travel.orderservice.internal.outbox.OutboxWriter
import com.travel.orderservice.internal.outbox.OutboxWriterImpl
import com.travel.orderservice.internal.persistence.OrderOutboxRepository
import com.travel.orderservice.internal.persistence.OrderRepository
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.retry.annotation.EnableRetry
import java.util.concurrent.atomic.AtomicInteger

// order-service has no @SpringBootApplication of its own (it's a library
// assembled by app), so @DataJpaTest needs an explicit bootstrap config to
// anchor entity/repository scanning -- same shape as
// BookingConcurrencyTestConfig in booking-service.
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableRetry
@EntityScan(basePackages = ["com.travel.orderservice.internal.persistence"])
@EnableJpaRepositories(basePackages = ["com.travel.orderservice.internal.persistence"])
class OrderConcurrencyTestConfig {
    @Bean
    fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()

    @Bean
    fun orderMetrics(registry: MeterRegistry) = OrderMetrics(registry)

    @Bean
    fun outboxWriter(repository: OrderOutboxRepository): OutboxWriter = OutboxWriterImpl(repository)

    // The real @Retryable/@Transactional-annotated bean, not a hand-built
    // instance -- part of the point of this test is proving what Spring's
    // proxies actually do under concurrent load, which a plain `new` skips.
    @Bean
    fun orderTransitions(orders: OrderRepository, outbox: OutboxWriter, metrics: OrderMetrics) =
        OrderTransitions(orders, outbox, metrics)

    // A controllable fake, not a Mockito mock -- proves order-service's own
    // duplicate prevention (P1/P2/P4), not Duffel's, and survives being
    // called concurrently from 20 real threads without any Mockito
    // thread-safety questions.
    @Bean
    fun duffelBookingService(): DuffelBookingService = FakeDuffelBookingService()

    @Bean
    fun orderService(
        orders: OrderRepository,
        gateway: DuffelBookingService,
        outbox: OutboxWriter,
        metrics: OrderMetrics,
        transitions: OrderTransitions,
    ): OrderService = OrderServiceImpl(orders, gateway, outbox, metrics, transitions)
}

class FakeDuffelBookingService : DuffelBookingService {
    val calls = AtomicInteger(0)

    override fun createBooking(request: BookingRequest, idempotencyKey: String, orderId: String?): BookingResult {
        calls.incrementAndGet()
        // Widen the window so 20 threads racing the same key actually
        // overlap inside this call, instead of running near-sequentially.
        Thread.sleep(50)
        return BookingResult(
            orderId = "duf_order_1",
            bookingReference = "ABC123",
            status = "confirmed",
            totalAmount = Money(amount = 199.99, currency = "USD"),
            eTicketNumbers = listOf("1234567890"),
            passengers = listOf(BookedPassenger(id = "pas_1", givenName = "Alex", familyName = "Doe")),
            emails = listOf("alex@example.com"),
        )
    }
}
