-- PaymentServiceImpl.createOrder() derives idempotency_key as "booking:<uuid>"
-- (8 + 36 = 44 chars), which never fit in the original VARCHAR(40) -- that
-- path was unwired until now, so nothing had ever actually inserted a row
-- through it to surface the mismatch.
ALTER TABLE payments ALTER COLUMN idempotency_key TYPE VARCHAR(80);
