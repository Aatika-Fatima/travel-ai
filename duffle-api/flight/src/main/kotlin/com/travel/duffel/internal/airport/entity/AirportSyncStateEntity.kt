package com.travel.duffel.internal.airport.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "airport_sync_state")
class AirportSyncStateEntity(
    @Id
    @Column(name = "job_name")
    var jobName: String,
    @Column(name = "last_cursor")
    var lastCursor: String? = null,
    @Column(name = "status", nullable = false)
    var status: String = "IDLE",
    @Column(name = "last_run_started_at")
    var lastRunStartedAt: Instant? = null,
    @Column(name = "last_run_finished_at")
    var lastRunFinishedAt: Instant? = null,
    @Column(name = "last_error")
    var lastError: String? = null,
)
