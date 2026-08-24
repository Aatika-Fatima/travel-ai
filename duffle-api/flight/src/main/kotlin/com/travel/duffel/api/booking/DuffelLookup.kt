package com.travel.duffel.api.booking

// A second, narrower capability alongside DuffelBookingService (P3) -- the
// reconciliation sweep needs to ask "does an order already exist for this
// key," not create one, so it gets its own interface rather than a new
// method bolted onto DuffelBookingService.
interface DuffelLookup {
    // Duffel's own order id if a match is found, null if no order has been
    // created against this metadata key yet -- ReconciliationJob only ever
    // checks which of those two cases it got.
    fun findByMetadata(key: String, value: String): String?
}
