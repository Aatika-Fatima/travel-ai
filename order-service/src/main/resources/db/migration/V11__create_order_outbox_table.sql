-- Named order_outbox -- distinct from notification's own booking_outbox
-- (V4) and booking-service's booking_event_outbox (V7), both unrelated
-- outboxes already living in this same shared database.
CREATE TABLE order_outbox (
    id            BIGSERIAL PRIMARY KEY,
    aggregate_id  UUID NOT NULL REFERENCES orders(id),
    event_type    VARCHAR(64) NOT NULL,
    payload       JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ
);

-- Partial index -- only ever scans the rows the relay in P7 actually cares about
CREATE INDEX idx_order_outbox_unpublished ON order_outbox (id) WHERE published_at IS NULL;
