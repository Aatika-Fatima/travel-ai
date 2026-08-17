package com.travel.notification.internal.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "email_delivery",
    uniqueConstraints = [
        UniqueConstraint(name = "ux_email_delivery_email_id", columnNames = ["email_id"]),
    ],
)
class EmailDeliveryEntity(
    @Column(name = "email_id", nullable = false)
    var emailId: UUID,
    @Column(name = "status", nullable = false, length = 16)
    var status: String,
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    var failureReason: String? = null,

    @Column(name = "sent_at")
    var sentAt: Instant? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}