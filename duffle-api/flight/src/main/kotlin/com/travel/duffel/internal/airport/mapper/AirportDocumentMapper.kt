package com.travel.duffel.internal.airport.mapper

import com.travel.common.entity.AirportEntity
import com.travel.duffel.internal.airport.document.AirportDocument
import org.springframework.data.elasticsearch.core.geo.GeoPoint

object AirportDocumentMapper {
    fun toDocument(entity: AirportEntity): AirportDocument =
        AirportDocument(
            iataCode = entity.iataCode,
            icaoCode = entity.icaoCode,
            name = entity.name,
            cityName = entity.cityName,
            iataCountryCode = entity.iataCountryCode,
            location = entity.latitude?.let { lat ->
                entity.longitude?.let { lon -> GeoPoint(lat, lon) }
            },
            lastSyncedAt = entity.lastSyncedAt,
        )
}