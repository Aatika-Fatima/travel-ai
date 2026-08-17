package com.travel.duffel.internal.airport.service.sync

data class AirportSyncSummary(
    val fetched: Int,
    val upserted: Int,
    val unchanged: Int,
    val skipped: Int,
    val durationMs: Long,
    val resumedFromCursor: String?,
)
