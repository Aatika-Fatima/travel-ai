package com.travel.common.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "airports",
    uniqueConstraints = [
        UniqueConstraint(name = "ux_airports_duffel_id", columnNames = ["duffel_id"]),
        UniqueConstraint(name = "ux_airports_iata_code", columnNames = ["iata_code"]),
    ],
)
class AirportEntity(
    @Column(name = "duffel_id", nullable = false)
    var duffelId: String,
    @Column(name = "iata_code", nullable = false, length = 3)
    var iataCode: String,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "icao_code", length = 4)
    var icaoCode: String? = null,
    @Column(name = "city_name")
    var cityName: String? = null,
    @Column(name = "iata_city_code", length = 3)
    var iataCityCode: String? = null,
    @Column(name = "iata_country_code", length = 2)
    var iataCountryCode: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    @Column(name = "time_zone")
    var timeZone: String? = null,
    @Column(name = "last_synced_at", nullable = false)
    var lastSyncedAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
