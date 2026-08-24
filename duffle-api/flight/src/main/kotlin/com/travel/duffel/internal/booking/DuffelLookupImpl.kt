package com.travel.duffel.internal.booking

import com.travel.duffel.api.booking.DuffelLookup
import com.travel.duffel.internal.client.DuffelOrderClient
import org.springframework.stereotype.Component

@Component
class DuffelLookupImpl(private val duffelOrderClient: DuffelOrderClient) : DuffelLookup {
    // Deliberately doesn't reuse DuffelBookingServiceImpl's private
    // toBookingResult() mapper -- that function also needs the passenger
    // emails list, which a metadata-only lookup doesn't have. Returning
    // just Duffel's order id keeps this path honest about what a lookup
    // alone can actually reconstruct.
    override fun findByMetadata(key: String, value: String): String? =
        duffelOrderClient.findOrdersByMetadata(key, value).firstOrNull()?.id
}
