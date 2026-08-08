package com.travel.duffel.airport.repository

import com.travel.common.entity.AirportEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@ContextConfiguration(classes = [AirportRepositoryTestConfig::class])
class AirportRepositoryTest {
    @Autowired
    lateinit var airportRepository: AirportRepository

    @BeforeEach
    fun seed() {
        val now = Instant.now()
        airportRepository.saveAll(
            listOf(
                AirportEntity(
                    duffelId = "arp_lhr_gb",
                    iataCode = "LHR",
                    name = "Heathrow",
                    cityName = "London",
                    iataCountryCode = "GB",
                    lastSyncedAt = now,
                ),
                AirportEntity(
                    duffelId = "arp_lgw_gb",
                    iataCode = "LGW",
                    name = "Gatwick",
                    cityName = "London",
                    iataCountryCode = "GB",
                    lastSyncedAt = now,
                ),
                AirportEntity(
                    duffelId = "arp_cdg_fr",
                    iataCode = "CDG",
                    name = "Charles de Gaulle",
                    cityName = "Paris",
                    iataCountryCode = "FR",
                    lastSyncedAt = now,
                ),
                AirportEntity(
                    duffelId = "arp_jfk_us",
                    iataCode = "JFK",
                    name = "John F Kennedy",
                    cityName = "New York",
                    iataCountryCode = "US",
                    lastSyncedAt = now,
                ),
            ),
        )
    }

    @Test
    fun `exact IATA code match ranks first`() {
        val results = airportRepository.search("LHR", PageRequest.of(0, 20))

        assertEquals("LHR", results.first().iataCode)
    }

    @Test
    fun `city prefix search returns all matching airports`() {
        val results = airportRepository.search("Lon", PageRequest.of(0, 20))

        assertEquals(setOf("LHR", "LGW"), results.map { it.iataCode }.toSet())
    }

    @Test
    fun `name or city prefix search is scoped to matching airports only`() {
        val results = airportRepository.search("Par", PageRequest.of(0, 20))

        assertEquals(listOf("CDG"), results.map { it.iataCode })
    }

    @Test
    fun `unmatched search terms return no results`() {
        val results = airportRepository.search("zzz", PageRequest.of(0, 20))

        assertTrue(results.isEmpty())
    }
}
