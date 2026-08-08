package com.travel.duffel.airport

data class AirportSyncSummary(
    val fetched: Int,
    val upserted: Int,
    val unchanged: Int,
    val skipped: Int,
    val durationMs: Long,
    val resumedFromCursor: String?,
)
