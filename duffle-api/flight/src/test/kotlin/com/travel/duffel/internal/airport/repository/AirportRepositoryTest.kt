package com.travel.duffel.internal.airport.repository

import com.travel.common.entity.AirportEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// `search` relies on pg_trgm (the `%` operator, `similarity()`) via a native query — the module's
// default @DataJpaTest database is H2 (see src/test/resources/application.yaml), which has neither,
// so this test runs against a real Postgres container instead, with Flyway actually applying
// V1__create_airports_table.sql (that's where the pg_trgm extension and trigram indexes come from).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"])
@ContextConfiguration(classes = [AirportRepositoryTestConfig::class])
@Testcontainers
class AirportRepositoryTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")
    }

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

    @Test
    fun `a misspelled city name still finds the airport via trigram similarity`() {
        // "Heathrw" isn't a prefix of "Heathrow" (drops the 'o'), so only trigram similarity finds it.
        val results = airportRepository.search("Heathrw", PageRequest.of(0, 20))

        assertEquals("LHR", results.first().iataCode)
    }

    @Test
    fun `a misspelled airport name still ranks ahead of unrelated airports`() {
        // "Gatwik" swaps out the 'c', so it's not a prefix of "Gatwick" either.
        val results = airportRepository.search("Gatwik", PageRequest.of(0, 20))

        assertEquals("LGW", results.first().iataCode)
    }
}
