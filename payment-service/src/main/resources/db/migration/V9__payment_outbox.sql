CREATE TABLE payment_outbox (
    id            BIGSERIAL PRIMARY KEY,
    event_id      UUID NOT NULL,
    event_type    VARCHAR(64) NOT NULL,
    booking_id    UUID NOT NULL,
    payload       JSONB NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ,

    CONSTRAINT uq_payment_outbox_event_id UNIQUE (event_id)
);

-- The publisher's poll query, and lookups by booking (tests, support).
CREATE INDEX ix_payment_outbox_status_created_at ON payment_outbox (status, created_at);
CREATE INDEX ix_payment_outbox_booking_id ON payment_outbox (booking_id);
