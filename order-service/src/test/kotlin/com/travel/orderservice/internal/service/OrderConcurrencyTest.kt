package com.travel.orderservice.internal.service

import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import com.travel.duffel.api.booking.DuffelBookingService
import com.travel.orderservice.api.OrderStatus
import com.travel.orderservice.api.SubmitOrderCommand
import com.travel.orderservice.internal.persistence.OrderOutboxRepository
import com.travel.orderservice.internal.persistence.OrderRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

// The runnable proof behind this module's own reason for existing (see "The
// gap this closes" and §P10 in order_service.html): N callers racing the
// same idempotency key against a real (H2, but constraint-backed) database
// produce exactly one order row and call Duffel exactly once -- the
// original stateless DuffelBookingServiceImpl would have failed this by a
// factor of `threadCount`. Twenty threads, not two -- a two-thread version
// can pass by luck on a fast machine even with the unique constraint
// accidentally removed.
@DataJpaTest
@ContextConfiguration(classes = [OrderConcurrencyTestConfig::class])
class OrderConcurrencyTest {
    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var orderOutboxRepository: OrderOutboxRepository

    @Autowired
    lateinit var orderService: OrderService

    @Autowired
    lateinit var duffelBookingService: DuffelBookingService

    private fun command(idempotencyKey: String) =
        SubmitOrderCommand(
            idempotencyKey = idempotencyKey,
            offerId = "off_race",
            passengers =
                listOf(
                    PassengerDetails(
                        title = "Mr",
                        givenName = "Alex",
                        familyName = "Doe",
                        dateOfBirth = LocalDate.of(1990, 1, 1),
                        gender = "m",
                        email = "alex@example.com",
                        phoneNumber = "+10000000000",
                    ),
                ),
            contact = ContactInfo(email = "alex@example.com", phoneNumber = "+10000000000"),
        )

    @Test
    fun `20 concurrent submits with the same idempotency key create exactly one order and call Duffel exactly once`() {
        val threadCount = 20
        val key = "idem-race-${Uuid.random()}"
        val cmd = command(key)
        val pool = Executors.newFixedThreadPool(threadCount)
        // Every worker blocks here until every other worker has also
        // reached this line, then all 20 are released in the same instant --
        // without this, a fixed thread pool tends to run tasks in near
        // sequence, and the race this test exists to provoke might just...
        // not happen.
        val allReady = CountDownLatch(threadCount)
        val go = CountDownLatch(1)

        val futures =
            (1..threadCount).map {
                pool.submit(
                    Callable {
                        allReady.countDown()
                        go.await(10, TimeUnit.SECONDS)
                        runCatching { orderService.submit(cmd) }
                    },
                )
            }
        allReady.await(10, TimeUnit.SECONDS)
        go.countDown()
        val results = futures.map { it.get(20, TimeUnit.SECONDS) }
        pool.shutdown()

        val succeeded = results.mapNotNull { it.getOrNull() }
        val failed = results.mapNotNull { it.exceptionOrNull() }
        failed.forEach { assertTrue(it is DataIntegrityViolationException, "unexpected failure: $it") }

        val fake = duffelBookingService as FakeDuffelBookingService
        assertEquals(1, fake.calls.get(), "Duffel is called exactly once")
        assertEquals(1, orderRepository.findAll().count { it.idempotencyKey == key }, "exactly one persisted order row")
        assertTrue(succeeded.isNotEmpty(), "at least one caller must win the race")
        assertEquals(1, succeeded.count { it.justCreated }, "exactly one caller sees justCreated")
        assertEquals(1, succeeded.map { it.orderId }.toSet().size, "every successful caller agrees on the same order")
        assertEquals(
            OrderStatus.CONFIRMED,
            orderRepository.findAll().first { it.idempotencyKey == key }.status,
            "the winning order was advanced all the way to CONFIRMED",
        )

        val orderId = succeeded.first { it.justCreated }.orderId
        assertEquals(
            1,
            orderOutboxRepository.findAll().count { it.aggregateId == orderId && it.eventType == "OrderPending" },
            "exactly one OrderPending outbox row",
        )
        assertEquals(
            1,
            orderOutboxRepository.findAll().count { it.aggregateId == orderId && it.eventType == "OrderConfirmed" },
            "exactly one OrderConfirmed outbox row",
        )
    }
}
