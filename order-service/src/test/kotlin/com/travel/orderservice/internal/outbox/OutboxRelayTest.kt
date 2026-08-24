package com.travel.orderservice.internal.outbox

import com.travel.orderservice.internal.persistence.OrderOutboxEntity
import com.travel.orderservice.internal.persistence.OrderOutboxRepository
import com.travel.orderservice.internal.service.OrderMetrics
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture
import kotlin.uuid.Uuid

class OutboxRelayTest {
    private val outbox: OrderOutboxRepository = mock()
    private val kafkaTemplate: KafkaTemplate<String, String> = mock()
    private val metrics: OrderMetrics = mock()
    private val relay = OutboxRelay(outbox, kafkaTemplate, metrics)

    private fun row(id: Long = 1L, eventType: String = "OrderConfirmed", aggregateId: Uuid = Uuid.random()) =
        OrderOutboxEntity(aggregateId = aggregateId, eventType = eventType, payload = "{}").apply { this.id = id }

    @Test
    fun `relay does nothing when there are no unpublished rows, but still records the backlog`() {
        whenever(outbox.lockNextBatch(100)).thenReturn(emptyList())
        whenever(outbox.countByPublishedAtIsNull()).thenReturn(0L)

        relay.relay()

        verify(kafkaTemplate, never()).send(any(), any(), any())
        verify(metrics).recordOutboxBacklog(0L)
    }

    @Test
    fun `relay sends each row keyed by aggregate id and marks it published once Kafka acknowledges it`() {
        val outboxRow = row()
        whenever(outbox.lockNextBatch(100)).thenReturn(listOf(outboxRow))
        whenever(outbox.countByPublishedAtIsNull()).thenReturn(0L)
        val future = CompletableFuture.completedFuture(mock<SendResult<String, String>>())
        whenever(kafkaTemplate.send("order.events", outboxRow.aggregateId.toString(), outboxRow.payload)).thenReturn(future)

        relay.relay()

        verify(outbox).markPublished(eq(outboxRow.id!!), any())
        verify(metrics, never()).publishFailure(any())
    }

    @Test
    fun `relay records a publish-failure metric, and never marks the row published, when the Kafka send fails`() {
        val outboxRow = row(eventType = "OrderPending")
        whenever(outbox.lockNextBatch(100)).thenReturn(listOf(outboxRow))
        whenever(outbox.countByPublishedAtIsNull()).thenReturn(1L)
        val failed = CompletableFuture<SendResult<String, String>>()
        failed.completeExceptionally(RuntimeException("broker unavailable"))
        whenever(kafkaTemplate.send("order.events", outboxRow.aggregateId.toString(), outboxRow.payload)).thenReturn(failed)

        relay.relay()

        verify(metrics).publishFailure("OrderPending")
        verify(outbox, never()).markPublished(any(), any())
    }

    @Test
    fun `relay processes every row in the batch, not just the first, and records the post-batch backlog`() {
        val first = row(id = 1L)
        val second = row(id = 2L)
        whenever(outbox.lockNextBatch(100)).thenReturn(listOf(first, second))
        whenever(outbox.countByPublishedAtIsNull()).thenReturn(3L)
        whenever(kafkaTemplate.send(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mock()))

        relay.relay()

        verify(outbox).markPublished(eq(1L), any())
        verify(outbox).markPublished(eq(2L), any())
        verify(metrics).recordOutboxBacklog(3L)
    }
}
