package com.travel.common.model

import java.time.LocalDateTime

data class Slice(
    val origin: Airport,
    val destination: Airport,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val durationMinutes: Long,
    val segments: List<Segment>,
) {
    val stops: Int get() = (segments.size - 1).coerceAtLeast(0)
}
