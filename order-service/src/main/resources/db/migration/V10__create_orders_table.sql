CREATE TABLE orders (
    id                  UUID PRIMARY KEY,
    idempotency_key     VARCHAR(255) NOT NULL,
    offer_id            VARCHAR(255) NOT NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'PENDING_SUBMISSION',
    duffel_order_id     VARCHAR(255),
    booking_reference   VARCHAR(16),
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- the source of truth for zero duplication, per UC1/UC2 -- everything else
    -- (the Redis lock in P2) is a latency optimization on top of this line
    CONSTRAINT uq_orders_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_orders_status ON orders (status);
