
package com.travel.duffel.internal.airport.service

import com.travel.duffel.internal.airport.mapper.AirportDocumentMapper
import com.travel.duffel.internal.airport.document.AirportDocumentRepository
import com.travel.duffel.internal.airport.repository.AirportRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class AirportReindexService(
    private val airportRepository: AirportRepository,
    // Absent under the `aws` profile, where Elasticsearch is not deployed (see AirportElasticSearchConfig).
    private val airportDocumentRepository: AirportDocumentRepository? = null,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Pages through every row in Postgres and bulk-writes it into Elasticsearch. Used for the
     * initial backfill and for manual recovery — routine syncs stay incremental via [AirportUpsertService]. */
    fun reindexAll(pageSize: Int = 500): Int {
        val documentRepository = airportDocumentRepository
            ?: throw UnsupportedOperationException("Elasticsearch is not enabled (aws profile); airport reindex is unavailable")
        var page = 0
        var total = 0
        while (true) {
            val chunk = airportRepository.findAll(PageRequest.of(page, pageSize))
            if (chunk.isEmpty) break

            documentRepository.saveAll(chunk.content.map(AirportDocumentMapper::toDocument))
            total += chunk.numberOfElements
            log.info("Reindexed {} airports so far (page {})", total, page)
            page++
        }
        return total
    }
}