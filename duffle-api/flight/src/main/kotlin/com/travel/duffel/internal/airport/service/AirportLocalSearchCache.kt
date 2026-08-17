package com.travel.duffel.internal.airport.service

import com.travel.common.model.AirportSummary
import com.travel.duffel.internal.airport.repository.AirportRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

// TODO: an Elasticsearch-backed fast path (AirportElasticSearchService) was referenced here
// but never implemented, so this currently only queries Postgres via AirportRepository.
@Service
class AirportLocalSearchCache(
    private val airportRepository: AirportRepository,
) {
    @Cacheable(value = ["airportSearch"], key = "#term")
    fun search(term: String): List<AirportSummary> =
        airportRepository
            .search(term, PageRequest.of(0, 20))
            .map { AirportSummary(it.iataCode, it.name, it.cityName, it.iataCountryCode, "local") }
}