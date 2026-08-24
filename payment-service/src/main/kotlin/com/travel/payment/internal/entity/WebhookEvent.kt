package com.travel.payment.internal.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

    /*
CREATE TABLE webhook_events (
    id            BIGSERIAL PRIMARY KEY,
    event         VARCHAR(64) NOT NULL,
    entity_id     VARCHAR(64) NOT NULL,
    payload       JSONB NOT NULL,
    received_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- The dedup key: Razorpay does not guarantee a stable delivery id, but
    -- (event type, entity id) is unique for the events this service acts
    -- on -- "payment.captured" can only ever be true once per payment id.
    CONSTRAINT uq_webhook_events_event_entity UNIQUE (event, entity_id)
);
 */

@Entity
@Table(name = "webhook_events")
class WebhookEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false) val event: String,
    @Column(name = "entity_id", nullable = false) val entityId: String,

    // Raw JSON payload Razorpay sent, kept verbatim for replay/debugging --
    // never parsed back out of this column, only written once and read as
    // a whole. Maps to the jsonb column via Hibernate's native JSON support.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val payload: String,

    @Column(name = "received_at", nullable = false) val receivedAt: Instant = Instant.now(),
)
