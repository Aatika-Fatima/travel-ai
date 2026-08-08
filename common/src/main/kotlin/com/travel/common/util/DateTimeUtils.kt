package com.travel.common.util

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val ISO_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun formatDate(date: LocalDate): String = ISO_DATE.format(date)

    fun parseDate(value: String): LocalDate = LocalDate.parse(value, ISO_DATE)

    fun formatDateTime(dateTime: OffsetDateTime): String = ISO_DATE_TIME.format(dateTime)

    fun parseDateTime(value: String): OffsetDateTime = OffsetDateTime.parse(value, ISO_DATE_TIME)

    fun durationMinutesBetween(
        start: OffsetDateTime,
        end: OffsetDateTime,
    ): Long = java.time.Duration.between(start, end).toMinutes()
}
