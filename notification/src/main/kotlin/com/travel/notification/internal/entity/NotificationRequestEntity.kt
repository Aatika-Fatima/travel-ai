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
    name = "notification_request",
    uniqueConstraints = [
        UniqueConstraint(name = "ux_notification_request_notification_id", columnNames = ["notification_id"]),
    ],
)
class NotificationRequestEntity(
    @Column(name = "notification_id", nullable = false)
    var notificationId: UUID,
    @Column(name = "channel", nullable = false, length = 16)
    var channel: String,
    @Column(name = "template", nullable = false, length = 64)
    var template: String,
    @Column(name = "recipient", nullable = false, length = 255)
    var recipient: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}