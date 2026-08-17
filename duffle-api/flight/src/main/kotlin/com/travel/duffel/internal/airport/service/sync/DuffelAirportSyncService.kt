package com.travel.duffel.internal.airport.service.sync

import com.travel.common.exception.ValidationException
import com.travel.duffel.internal.client.DuffelAirportClient
import com.travel.duffel.internal.config.sync.DuffelAirportSyncProperties
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Deliberately not `@Transactional` itself: each page's commit (via [AirportUpsertService])
 * must be durable independent of later pages, so a failure on page N doesn't roll back the
 * upserts already committed for pages 1..N-1.
 */
@Service
class DuffelAirportSyncService(
    private val duffelAirportClient: DuffelAirportClient,
    private val airportUpsertService: AirportUpsertService,
    private val syncProperties: DuffelAirportSyncProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @CacheEvict(cacheNames = ["airportSearch"], allEntries = true)
    fun syncAllAirports(iataCountryCode: String? = null): AirportSyncSummary {
        val startState =
            try {
                airportUpsertService.markRunning(JOB_NAME)
            } catch (ex: IllegalStateException) {
                throw ValidationException(
                    "Airport sync '$JOB_NAME' is already running",
                    errors = listOf("sync_in_progress"),
                )
            }

        val resumedFrom = startState.lastCursor
        var cursor = resumedFrom
        val start = Instant.now()
        var fetched = 0
        var upserted = 0
        var unchanged = 0
        var skipped = 0

        try {
            do {
                val page =
                    duffelAirportClient.fetchAirports(
                        after = cursor,
                        limit = syncProperties.pageSize,
                        iataCountryCode = iataCountryCode,
                    )
                fetched += page.data.size

                val result = airportUpsertService.upsertPage(JOB_NAME, page.data, page.meta.after)
                upserted += result.upserted
                unchanged += result.unchanged
                skipped += result.skipped

                cursor = page.meta.after
            } while (!cursor.isNullOrBlank())

            airportUpsertService.markFinished(JOB_NAME, success = true, error = null)
        } catch (ex: Exception) {
            airportUpsertService.markFinished(JOB_NAME, success = false, error = ex.message)
            log.error(
                "Duffel airport sync failed after fetched={} upserted={} unchanged={}",
                fetched,
                upserted,
                unchanged,
                ex,
            )
            throw ex
        }

        val summary =
            AirportSyncSummary(
                fetched = fetched,
                upserted = upserted,
                unchanged = unchanged,
                skipped = skipped,
                durationMs = Duration.between(start, Instant.now()).toMillis(),
                resumedFromCursor = resumedFrom,
            )
        log.info("Duffel airport sync complete: {}", summary)
        return summary
    }

    companion object {
        const val JOB_NAME = "duffel-airport-sync"
    }
}
