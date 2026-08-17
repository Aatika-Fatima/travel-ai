package com.travel.duffel.internal.airport.repository

import com.travel.duffel.internal.airport.entity.AirportSyncStateEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AirportSyncStateRepository : JpaRepository<AirportSyncStateEntity, String>
