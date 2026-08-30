-- When Duffel rejects an order (422 / payment declined / offer gone), the saga
-- moves the order to FAILED. Persist *why* so a failed booking is debuggable
-- from the row alone -- the customer only ever sees "the airline could not
-- confirm this booking".
ALTER TABLE orders
    ADD COLUMN failure_reason VARCHAR(500);
