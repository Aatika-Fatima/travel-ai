package com.travel.orderservice.internal.persistence

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.uuid.Uuid

@DataJpaTest
@ContextConfiguration(classes = [OrderOutboxRepositoryTestConfig::class])
class OrderOutboxRepositoryTest {
    @Autowired
    lateinit var repository: OrderOutboxRepository

    private fun row(aggregateId: Uuid = Uuid.random()) =
        OrderOutboxEntity(aggregateId = aggregateId, eventType = "OrderConfirmed", payload = "{}")

    @Test
    fun `lockNextBatch returns only unpublished rows, oldest first`() {
        val first = repository.save(row())
        Thread.sleep(5)
        val second = repository.save(row())
        val published = repository.save(row())
        published.publishedAt = Clock.System.now()
        repository.save(published)

        val batch = repository.lockNextBatch(10)

        assertEquals(listOf(first.id, second.id), batch.map { it.id })
    }

    @Test
    fun `lockNextBatch respects the given limit`() {
        repeat(3) { repository.save(row()) }

        val batch = repository.lockNextBatch(2)

        assertEquals(2, batch.size)
    }

    @Test
    fun `markPublished stamps publishedAt on exactly the given row`() {
        val saved = repository.save(row())
        val other = repository.save(row())
        val now = Clock.System.now()

        repository.markPublished(saved.id!!, now)

        assertEquals(now, repository.findById(saved.id!!).orElseThrow().publishedAt)
        assertEquals(null, repository.findById(other.id!!).orElseThrow().publishedAt)
    }

    @Test
    fun `countByPublishedAtIsNull counts only unpublished rows`() {
        repository.save(row())
        repository.save(row())
        val published = repository.save(row())
        repository.markPublished(published.id!!, Clock.System.now())

        assertEquals(2L, repository.countByPublishedAtIsNull())
    }
}
