CREATE TABLE booking_outbox (
    id            BIGSERIAL PRIMARY KEY,
    event_id      UUID NOT NULL UNIQUE,
    event_type    VARCHAR(64) NOT NULL,
    order_id      VARCHAR(64) NOT NULL,
    payload       JSONB NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ
);

CREATE INDEX idx_booking_outbox_pending
    ON booking_outbox (created_at)
    WHERE status = 'PENDING';