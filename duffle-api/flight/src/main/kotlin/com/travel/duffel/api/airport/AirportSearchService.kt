package com.travel.duffel.api.airport

import com.travel.common.exception.ValidationException
import com.travel.common.model.AirportSummary
import com.travel.duffel.internal.airport.service.AirportLocalSearchCache
import com.travel.duffel.internal.client.DuffelPlacesClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AirportSearchService(
    private val airportLocalSearchService: AirportLocalSearchCache,
    private val duffelPlacesClient: DuffelPlacesClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun search(query: String): List<AirportSummary> {
        val term = query.trim()
        if (term.isEmpty()) {
            throw ValidationException("Search term must not be blank", errors = listOf("q: must not be blank"))
        }

        val local = airportLocalSearchService.search(term)
        if (local.isNotEmpty()) {
            return local.map { AirportSummary(it.iataCode, it.name, it.cityName, it.iataCountryCode, "local") }
        }

        return runCatching { duffelPlacesClient.suggest(term) }
            .onFailure { log.warn("Duffel Places fallback failed for '{}': {}", term, it.message) }
            .getOrDefault(emptyList())
            .filter { it.type == "airport" && !it.iataCode.isNullOrBlank() }
            .map { AirportSummary(it.iataCode!!, it.name.orEmpty(), it.cityName, it.iataCountryCode, "duffel-live") }
            .sortedBy { relevanceRank(it, term) }
    }

    // Duffel's own suggest ranking doesn't reliably put the airport whose city name is an exact
    // match first (e.g. "Mumbai" -> Navi Mumbai (NMI) ahead of the actual Mumbai airport, BOM).
    // Mirrors the exact-code tie-break the local DB query already does via ORDER BY.
    private fun relevanceRank(
        airport: AirportSummary,
        term: String,
    ): Int =
        when {
            airport.iataCode.equals(term, ignoreCase = true) -> 0
            airport.cityName.equals(term, ignoreCase = true) -> 1
            else -> 2
        }
}