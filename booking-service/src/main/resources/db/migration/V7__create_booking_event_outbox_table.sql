-- Named booking_event_outbox, not booking_outbox -- notification's own
-- V4 migration already created a booking_outbox table (its own, unrelated
-- outbox) in this same shared database. A shared name here would collide
-- at migration time.
CREATE TABLE booking_event_outbox (
    id            BIGSERIAL PRIMARY KEY,
    booking_id    UUID NOT NULL REFERENCES bookings(id),
    event_type    VARCHAR(64) NOT NULL,
    payload       JSONB NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ
);

-- Partial index -- the relay in P5 only ever scans PENDING rows, and this
-- keeps that scan an index-only lookup no matter how large the table gets.
CREATE INDEX idx_booking_event_outbox_pending ON booking_event_outbox (created_at) WHERE status = 'PENDING';
