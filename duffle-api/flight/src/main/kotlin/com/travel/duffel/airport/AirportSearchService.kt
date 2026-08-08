package com.travel.duffel.airport

import com.travel.common.exception.ValidationException
import com.travel.common.model.AirportSummary
import com.travel.duffel.airport.repository.AirportRepository
import com.travel.duffel.client.DuffelPlacesClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class AirportSearchService(
    private val airportRepository: AirportRepository,
    private val duffelPlacesClient: DuffelPlacesClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun search(query: String): List<AirportSummary> {
        val term = query.trim()
        if (term.isEmpty()) {
            throw ValidationException("Search term must not be blank", errors = listOf("q: must not be blank"))
        }

        val local = airportRepository.search(term, PageRequest.of(0, 20))
        if (local.isNotEmpty()) {
            return local.map { AirportSummary(it.iataCode, it.name, it.cityName, it.iataCountryCode, "local") }
        }

        return runCatching { duffelPlacesClient.suggest(term) }
            .onFailure { log.warn("Duffel Places fallback failed for '{}': {}", term, it.message) }
            .getOrDefault(emptyList())
            .filter { it.type == "airport" && !it.iataCode.isNullOrBlank() }
            .map { AirportSummary(it.iataCode!!, it.name.orEmpty(), it.cityName, it.iataCountryCode, "duffel-live") }
    }
}
