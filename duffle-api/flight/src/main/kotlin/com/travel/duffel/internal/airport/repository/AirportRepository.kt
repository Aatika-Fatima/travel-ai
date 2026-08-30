package com.travel.duffel.internal.airport.repository

import com.travel.common.entity.AirportEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AirportRepository : JpaRepository<AirportEntity, Long> {
    fun findByDuffelId(duffelId: String): AirportEntity?

    fun findByIataCode(iataCode: String): AirportEntity?

    // Exact IATA code, then prefix match, then pg_trgm similarity — the trigram GIN indexes
    // from V1__create_airports_table.sql exist for exactly this (typo/substring tolerant) fallback.
    // Native query: pg_trgm's `%` operator and `similarity()` have no JPQL equivalent.
    @Query(
        value = """
        SELECT a.* FROM airports a
        WHERE UPPER(a.iata_code) = UPPER(:term)
           OR UPPER(a.name) LIKE UPPER(CONCAT(:term, '%'))
           OR UPPER(a.city_name) LIKE UPPER(CONCAT(:term, '%'))
           OR a.name % :term
           OR a.city_name % :term
        ORDER BY
            CASE
                WHEN UPPER(a.iata_code) = UPPER(:term) THEN 0
                WHEN UPPER(a.name) LIKE UPPER(CONCAT(:term, '%'))
                  OR UPPER(a.city_name) LIKE UPPER(CONCAT(:term, '%')) THEN 1
                ELSE 2
            END,
            GREATEST(similarity(a.name, :term), similarity(COALESCE(a.city_name, ''), :term)) DESC,
            a.name
        """,
        nativeQuery = true,
    )
    fun search(
        @Param("term") term: String,
        pageable: Pageable,
    ): List<AirportEntity>
}
