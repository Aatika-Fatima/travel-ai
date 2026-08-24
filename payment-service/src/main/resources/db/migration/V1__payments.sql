CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id          UUID NOT NULL,
    idempotency_key     VARCHAR(40) NOT NULL,
    razorpay_order_id   VARCHAR(64),
    razorpay_payment_id VARCHAR(64),
    amount_paise        BIGINT NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    status              VARCHAR(16) NOT NULL,
    failure_reason      VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_payments_idempotency_key UNIQUE (idempotency_key)
);

-- The idempotency_key constraint stops a retried call from creating a
-- second row. This partial index is the separate guarantee that no
-- *booking* can ever have two live payment attempts at once, even if
-- someone generated two different keys for it by mistake — CAPTURED
-- stays unique forever; FAILED is excluded so a genuinely failed
-- payment can be retried with a brand-new attempt.
CREATE UNIQUE INDEX uq_payments_booking_active
    ON payments (booking_id)
    WHERE status IN ('CREATED', 'ATTEMPTED', 'CAPTURED');

CREATE INDEX ix_payments_razorpay_order_id ON payments (razorpay_order_id);

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