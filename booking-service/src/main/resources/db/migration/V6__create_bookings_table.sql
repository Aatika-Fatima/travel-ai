CREATE TABLE bookings (
    id                  UUID PRIMARY KEY,
    idempotency_key     VARCHAR(255) NOT NULL,
    offer_id            VARCHAR(255) NOT NULL,
    status              VARCHAR(24)  NOT NULL DEFAULT 'INITIATED',
    -- the full BookingRequest (offerId, passengers, contact, paymentType),
    -- serialized once at intake and never re-derived -- this JSONB blob is
    -- byte-for-byte what gets republished inside the BookingCreated event.
    request_payload     JSONB        NOT NULL,
    -- populated once a terminal FAILED transition is applied -- see P4.
    failure_reason      VARCHAR(255),
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Guarantee #1: no two bookings for the same logical request.
    CONSTRAINT uq_bookings_idempotency_key UNIQUE (idempotency_key)
);

-- Guarantee #2: no two *live* bookings for the same Duffel offer. An offer
-- is single-use -- once an order is created against it, it can't be
-- ordered again -- so this is a real domain rule, not just belt-and-braces.
-- The three terminal states (CANCELLED, FAILED, EXPIRED -- see P4's state
-- machine) are excluded so a booking that genuinely ended can free up its
-- offer_id; every other status is "still live" and holds the lock.
CREATE UNIQUE INDEX uq_bookings_offer_active
    ON bookings (offer_id)
    WHERE status NOT IN ('CANCELLED', 'FAILED', 'EXPIRED');

CREATE INDEX idx_bookings_status ON bookings (status);
