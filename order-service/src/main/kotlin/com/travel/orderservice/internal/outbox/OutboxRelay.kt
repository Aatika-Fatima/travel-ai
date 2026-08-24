package com.travel.orderservice.internal.outbox

import com.travel.orderservice.internal.persistence.OrderOutboxRepository
import com.travel.orderservice.internal.service.OrderMetrics
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock

@Component
class OutboxRelay(
    private val outbox: OrderOutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val metrics: OrderMetrics,
) {
    @Scheduled(fixedDelay = 500)
    @Transactional
    fun relay() {
        // FOR UPDATE SKIP LOCKED -- a second relay instance polling at the
        // same moment skips rows this one already has, instead of blocking
        // on them.
        val batch = outbox.lockNextBatch(100)
        for (row in batch) {
            // key = aggregate_id (the order id) -- every event for one
            // order lands on the same partition, so a consumer never sees
            // them out of order.
            kafkaTemplate.send("order.events", row.aggregateId.toString(), row.payload)
                .whenComplete { _, ex ->
                    if (ex == null) outbox.markPublished(row.id!!, Clock.System.now()) else metrics.publishFailure(row.eventType)
                }
        }
        metrics.recordOutboxBacklog(outbox.countByPublishedAtIsNull())
    }
}
