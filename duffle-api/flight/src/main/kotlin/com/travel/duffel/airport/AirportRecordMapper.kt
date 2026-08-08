package com.travel.duffel.airport

import com.travel.common.entity.AirportEntity
import com.travel.duffel.dto.response.DuffelAirportRecord
import java.time.Instant

object AirportRecordMapper {
    fun toNewEntity(
        record: DuffelAirportRecord,
        syncedAt: Instant,
    ): AirportEntity =
        AirportEntity(
            duffelId = record.id,
            iataCode = requireNotNull(record.iataCode) { "iataCode must not be null for a persisted airport" },
            name = record.name,
            icaoCode = record.icaoCode,
            cityName = record.cityName ?: record.city?.name,
            iataCityCode = record.iataCityCode,
            iataCountryCode = record.iataCountryCode,
            latitude = record.latitude,
            longitude = record.longitude,
            timeZone = record.timeZone,
            lastSyncedAt = syncedAt,
        )

    fun applyChanges(
        entity: AirportEntity,
        record: DuffelAirportRecord,
        syncedAt: Instant,
    ): Boolean {
        val cityName = record.cityName ?: record.city?.name
        var changed = false

        if (entity.iataCode != record.iataCode && record.iataCode != null) {
            entity.iataCode = record.iataCode
            changed = true
        }
        if (entity.name != record.name) {
            entity.name = record.name
            changed = true
        }
        if (entity.icaoCode != record.icaoCode) {
            entity.icaoCode = record.icaoCode
            changed = true
        }
        if (entity.cityName != cityName) {
            entity.cityName = cityName
            changed = true
        }
        if (entity.iataCityCode != record.iataCityCode) {
            entity.iataCityCode = record.iataCityCode
            changed = true
        }
        if (entity.iataCountryCode != record.iataCountryCode) {
            entity.iataCountryCode = record.iataCountryCode
            changed = true
        }
        if (entity.latitude != record.latitude) {
            entity.latitude = record.latitude
            changed = true
        }
        if (entity.longitude != record.longitude) {
            entity.longitude = record.longitude
            changed = true
        }
        if (entity.timeZone != record.timeZone) {
            entity.timeZone = record.timeZone
            changed = true
        }

        if (changed) {
            entity.lastSyncedAt = syncedAt
        }
        return changed
    }
}
