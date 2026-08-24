package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.persistence.BookingEventOutboxRepository
import com.travel.bookingservice.internal.persistence.BookingRepository
import com.travel.common.model.BookingRequest
import com.travel.common.model.ContactInfo
import com.travel.common.model.PassengerDetails
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The runnable proof behind P8's flagship claim (docs/booking_service.html
// #p8, `t-8-0`): N callers racing the exact same Idempotency-Key against a
// real (H2, but a real transactional/constraint-backed) database produce
// exactly one row and exactly one outbox event -- never a duplicate
// booking, no matter how the threads interleave. Twenty threads, not two --
// see #p8's own step-note: a two-thread version can pass by luck on a fast
// machine even with the unique constraint accidentally removed.
@DataJpaTest
@ContextConfiguration(classes = [BookingConcurrencyTestConfig::class])
class BookingConcurrencyTest {
    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var bookingOutboxRepository: BookingEventOutboxRepository

    @Autowired
    lateinit var transactionalOps: BookingTransactionalOps

    private fun request() =
        BookingRequest(
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
    fun `20 concurrent submits with the same idempotency key produce exactly one booking and one outbox row`() {
        val threadCount = 20
        val key = "bk-race-${UUID.randomUUID()}"
        val req = request()
        val pool = Executors.newFixedThreadPool(threadCount)
        // Every worker blocks here until every other worker has also
        // reached this line, then all 20 are released in the same instant --
        // without this, a fixed thread pool tends to run tasks in near
        // sequence, and the very race this test exists to provoke might
        // just... not happen.
        val allReady = CountDownLatch(threadCount)
        val go = CountDownLatch(1)

        val futures =
            (1..threadCount).map {
                pool.submit(
                    Callable {
                        allReady.countDown()
                        go.await(10, TimeUnit.SECONDS)
                        // A caller that loses the insert race AND can't yet see the
                        // winner's row on H2's own commit-visibility timing hits the
                        // documented, already-unit-tested "rethrow" fallback in
                        // findOrInsertPending -- a real, if DB-timing-dependent,
                        // outcome, not a bug in this test. What must never happen,
                        // regardless of how many callers hit that fallback, is a
                        // second persisted row or a second outbox event.
                        runCatching { transactionalOps.findOrInsertPending(key, req) }
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

        assertEquals(1, bookingRepository.findAll().count { it.idempotencyKey == key }, "exactly one persisted booking row")
        assertTrue(succeeded.isNotEmpty(), "at least one caller must win the race")
        assertEquals(1, succeeded.count { (_, alreadyExisted) -> !alreadyExisted }, "exactly one caller sees justCreated")
        assertEquals(1, succeeded.map { (booking, _) -> booking.bookingId }.toSet().size, "every successful caller agrees on the same row")

        val bookingId = succeeded.first { (_, alreadyExisted) -> !alreadyExisted }.first.bookingId
        assertEquals(1, bookingOutboxRepository.findAll().count { it.bookingId == bookingId }, "exactly one BookingCreated outbox row")
    }
}
