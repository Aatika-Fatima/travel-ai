package com.travel.orderservice.internal.outbox

import com.travel.orderservice.internal.service.OutboxEvent
import kotlin.uuid.Uuid

interface OutboxWriter {
    fun append(aggregateId: Uuid, event: OutboxEvent)
}
