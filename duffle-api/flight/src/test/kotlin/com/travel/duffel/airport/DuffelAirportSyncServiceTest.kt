package com.travel.duffel.airport

import com.github.tomakehurst.wiremock.client.WireMock.absent
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.travel.common.entity.AirportEntity
import com.travel.duffel.airport.entity.AirportSyncStateEntity
import com.travel.duffel.airport.repository.AirportRepository
import com.travel.duffel.airport.repository.AirportSyncStateRepository
import com.travel.duffel.client.DuffelAirportClient
import com.travel.duffel.config.DuffelAirportSyncProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DuffelAirportSyncServiceTest {
    companion object {
        @RegisterExtension
        @JvmStatic
        val wireMock: WireMockExtension = WireMockExtension.newInstance().build()
    }

    private fun duffelAirportClient(): DuffelAirportClient {
        val timeout = Duration.ofSeconds(2)
        val requestFactory =
            ClientHttpRequestFactoryBuilder
                .simple()
                .build(HttpClientSettings.defaults().withTimeouts(timeout, timeout))
        val restClient =
            RestClient
                .builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build()
        return DuffelAirportClient(restClient, JsonMapper.builder().build())
    }

    // `any(SomeClass::class.java)` returns null, which trips Kotlin's call-site null-check
    // when the target parameter is declared non-null in a Kotlin-authored interface (unlike
    // mocking a Java-declared interface, where the parameter is a platform type and no check
    // is inserted). `anyString()` sidesteps this for String params by returning "" instead of
    // null; for entity params, `any(...) ?: dummy` keeps Mockito's matcher recording (a side
    // effect of calling `any()`) while substituting a real, non-null instance for the actual
    // call so the check passes.
    private val dummyAirportEntity = AirportEntity(duffelId = "", iataCode = "", name = "", lastSyncedAt = Instant.EPOCH)
    private val dummyAirportSyncStateEntity = AirportSyncStateEntity(jobName = "")

    // Backed by an in-memory map keyed by duffelId, so lookups/saves behave like a real
    // repository row across multiple calls within a single sync run (not just one-shot stubs).
    private fun airportRepositoryFake(): AirportRepository {
        val byDuffelId = linkedMapOf<String, AirportEntity>()
        val repo = mock(AirportRepository::class.java)
        given(repo.findByDuffelId(anyString())).willAnswer { inv -> byDuffelId[inv.arguments[0] as String] }
        given(repo.findByIataCode(anyString())).willAnswer { inv ->
            val code = inv.arguments[0] as String
            byDuffelId.values.find { it.iataCode == code }
        }
        given(repo.save(any(AirportEntity::class.java) ?: dummyAirportEntity)).willAnswer { inv ->
            val entity = inv.arguments[0] as AirportEntity
            byDuffelId[entity.duffelId] = entity
            entity
        }
        return repo
    }

    // Backed by a single mutable slot, so mutations made by one call (e.g. upsertPage
    // advancing the cursor) are visible to the next (e.g. markFinished).
    private fun syncStateRepositoryFake(initial: AirportSyncStateEntity? = null): AirportSyncStateRepository {
        var state = initial
        val repo = mock(AirportSyncStateRepository::class.java)
        given(repo.findById(anyString())).willAnswer { Optional.ofNullable(state) }
        given(repo.save(any(AirportSyncStateEntity::class.java) ?: dummyAirportSyncStateEntity)).willAnswer { inv ->
            val entity = inv.arguments[0] as AirportSyncStateEntity
            state = entity
            entity
        }
        return repo
    }

    private fun syncService(
        airportRepository: AirportRepository,
        syncStateRepository: AirportSyncStateRepository,
    ): DuffelAirportSyncService {
        val upsertService = AirportUpsertService(airportRepository, syncStateRepository)
        return DuffelAirportSyncService(duffelAirportClient(), upsertService, DuffelAirportSyncProperties())
    }

    private fun airportJson(
        id: String,
        iataCode: String,
        name: String,
    ) = """{"id": "$id", "name": "$name", "iata_code": "$iataCode", "city_name": "City for $name"}"""

    @Test
    fun `pages through results and upserts without duplicating records`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/air/airports"))
                .withQueryParam("after", absent())
                .willReturn(
                    okJson(
                        """{"data":[${airportJson("arp_1", "AAA", "Airport A")},${airportJson("arp_2", "BBB", "Airport B")}],"meta":{"after":"cursor-2"}}""",
                    ),
                ),
        )
        wireMock.stubFor(
            get(urlPathEqualTo("/air/airports"))
                .withQueryParam("after", equalTo("cursor-2"))
                .willReturn(okJson("""{"data":[${airportJson("arp_1", "AAA", "Airport A")}],"meta":{}}""")),
        )

        val airportRepository = airportRepositoryFake()
        val syncStateRepository = syncStateRepositoryFake()

        val summary = syncService(airportRepository, syncStateRepository).syncAllAirports()

        assertEquals(3, summary.fetched)
        assertEquals(2, summary.upserted)
        assertEquals(1, summary.unchanged)
        verify(airportRepository, times(2)).save(any(AirportEntity::class.java) ?: dummyAirportEntity)
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/air/airports")).withQueryParam("after", absent()))
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/air/airports")).withQueryParam("after", equalTo("cursor-2")))
    }

    @Test
    fun `resumes from the persisted cursor instead of restarting from page 1`() {
        // Only the after=cursor-2 stub is registered. If the service incorrectly restarted
        // from page 1, WireMock would 404 the un-stubbed no-`after` request and fail this test.
        wireMock.stubFor(
            get(urlPathEqualTo("/air/airports"))
                .withQueryParam("after", equalTo("cursor-2"))
                .willReturn(okJson("""{"data":[${airportJson("arp_1", "AAA", "Airport A")}],"meta":{}}""")),
        )

        val airportRepository = airportRepositoryFake()
        val previousState =
            AirportSyncStateEntity(jobName = DuffelAirportSyncService.JOB_NAME, lastCursor = "cursor-2", status = "FAILED")
        val syncStateRepository = syncStateRepositoryFake(previousState)

        val summary = syncService(airportRepository, syncStateRepository).syncAllAirports()

        assertEquals("cursor-2", summary.resumedFromCursor)
        assertEquals(1, summary.fetched)
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/air/airports")).withQueryParam("after", absent()))
    }

    @Test
    fun `preserves the last committed cursor when a later page fails`() {
        wireMock.stubFor(
            get(urlPathEqualTo("/air/airports"))
                .withQueryParam("after", absent())
                .willReturn(okJson("""{"data":[${airportJson("arp_1", "AAA", "Airport A")}],"meta":{"after":"cursor-2"}}""")),
        )
        wireMock.stubFor(
            get(urlPathEqualTo("/air/airports"))
                .withQueryParam("after", equalTo("cursor-2"))
                .willReturn(aResponse().withStatus(500)),
        )

        val airportRepository = airportRepositoryFake()
        val syncStateRepository = syncStateRepositoryFake()

        assertFailsWith<Exception> { syncService(airportRepository, syncStateRepository).syncAllAirports() }

        val captor = ArgumentCaptor.forClass(AirportSyncStateEntity::class.java)
        verify(syncStateRepository, atLeastOnce()).save(captor.capture() ?: dummyAirportSyncStateEntity)
        val finalState = captor.allValues.last()
        assertEquals("FAILED", finalState.status)
        assertEquals("cursor-2", finalState.lastCursor)
    }
}
