package com.travel.duffel.internal.airport.service.sync

import com.travel.common.entity.AirportEntity
import com.travel.duffel.internal.airport.document.AirportDocumentRepository
import com.travel.duffel.internal.airport.entity.AirportSyncStateEntity
import com.travel.duffel.internal.airport.mapper.AirportDocumentMapper
import com.travel.duffel.internal.airport.mapper.AirportRecordMapper
import com.travel.duffel.internal.airport.repository.AirportRepository
import com.travel.duffel.internal.airport.repository.AirportSyncStateRepository
import com.travel.duffel.api.dto.response.DuffelAirportRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class PageUpsertResult(
    val upserted: Int,
    val unchanged: Int,
    val skipped: Int,
)

/**
 * Kept as a bean separate from [DuffelAirportSyncService]: a `@Transactional` method
 * calling itself from within the same class bypasses Spring's proxy and silently
 * runs without a transaction, so the transactional boundary must live on a different
 * bean than the pagination loop that calls it.
 */
@Service
class AirportUpsertService(
    private val airportRepository: AirportRepository,
    private val syncStateRepository: AirportSyncStateRepository,
    private val airportDocumentRepository: AirportDocumentRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)  // new

    @Transactional
    fun markRunning(jobName: String): AirportSyncStateEntity {
        val state = syncStateRepository.findById(jobName).orElseGet { AirportSyncStateEntity(jobName = jobName) }
        check(state.status != "RUNNING") { "sync_in_progress" }
        state.status = "RUNNING"
        state.lastRunStartedAt = Instant.now()
        state.lastError = null
        return syncStateRepository.save(state)
    }

    @Transactional
    fun upsertPage(
        jobName: String,
        records: List<DuffelAirportRecord>,
        nextCursor: String?,
    ): PageUpsertResult {
        var upserted = 0
        var unchanged = 0
        var skipped = 0
        val now = Instant.now()

        records.forEach { record ->
            if (record.iataCode.isNullOrBlank()) {
                skipped++
                return@forEach
            }

            val existing = airportRepository.findByDuffelId(record.id) ?: airportRepository.findByIataCode(record.iataCode)
            when {
                existing == null -> {
                    airportRepository.save(AirportRecordMapper.toNewEntity(record, now))
                    upserted++
                }
                AirportRecordMapper.applyChanges(existing, record, now) -> {
                    airportRepository.save(existing)
                    upserted++
                }
                else -> unchanged++
            }
        }

        val state = syncStateRepository.findById(jobName).orElseGet { AirportSyncStateEntity(jobName = jobName) }
        state.lastCursor = nextCursor
        syncStateRepository.save(state)

        return PageUpsertResult(upserted, unchanged, skipped)
    }

    @Transactional
    fun markFinished(
        jobName: String,
        success: Boolean,
        error: String?,
    ) {
        val state = syncStateRepository.findById(jobName).orElseGet { AirportSyncStateEntity(jobName = jobName) }
        state.status = if (success) "SUCCESS" else "FAILED"
        state.lastRunFinishedAt = Instant.now()
        state.lastError = error
        if (success) {
            state.lastCursor = null
        }
        syncStateRepository.save(state)
    }

    private fun indexOrLog(entity:AirportEntity){
        runCatching{
            airportDocumentRepository.save(AirportDocumentMapper.toDocument(entity))
        }.onFailure{
            log.warn("Elasticsearch index write failed for {}: {}", entity.iataCode, it.message)
        }

    }
}
