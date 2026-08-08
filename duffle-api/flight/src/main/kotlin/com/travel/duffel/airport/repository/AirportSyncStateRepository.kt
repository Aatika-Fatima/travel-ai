package com.travel.duffel.airport.repository

import com.travel.duffel.airport.entity.AirportSyncStateEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AirportSyncStateRepository : JpaRepository<AirportSyncStateEntity, String>
