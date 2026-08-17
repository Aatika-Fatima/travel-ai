package com.travel.duffel.internal.airport.repository

import com.travel.common.entity.AirportEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AirportRepository : JpaRepository<AirportEntity, Long> {
    fun findByDuffelId(duffelId: String): AirportEntity?

    fun findByIataCode(iataCode: String): AirportEntity?

    @Query(
        """
        SELECT a FROM AirportEntity a
        WHERE UPPER(a.iataCode) = UPPER(:term)
           OR UPPER(a.name) LIKE UPPER(CONCAT(:term, '%'))
           OR UPPER(a.cityName) LIKE UPPER(CONCAT(:term, '%'))
        ORDER BY CASE WHEN UPPER(a.iataCode) = UPPER(:term) THEN 0 ELSE 1 END, a.name
        """,
    )
    fun search(
        @Param("term") term: String,
        pageable: Pageable,
    ): List<AirportEntity>
}
