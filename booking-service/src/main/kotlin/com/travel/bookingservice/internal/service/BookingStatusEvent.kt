package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.persistence.BookingStatus
import java.awt.print.Book

enum class BookingStatusEvent {
    PUBLISHED,
    PROVIDER_RESERVED,
    PROVIDER_CONFIRMED,
    PROVIDER_FAILED,
    CANCEL_REQUESTED,
    CANCEL_CONFIRMED,
    EXPIRED
}
fun BookingStatus.transition(event: BookingStatusEvent): BookingStatus {
    return ALLOWED[this to event] ?: throw IllegalStateTransitionException(this, event)
 }
    val ALLOWED = mapOf(
    (BookingStatus.INITIATED to BookingStatusEvent.PUBLISHED) to BookingStatus.IN_PROGRESS,
    (BookingStatus.INITIATED to BookingStatusEvent.CANCEL_REQUESTED) to BookingStatus.CANCELLED,
    (BookingStatus.INITIATED to BookingStatusEvent.EXPIRED) to BookingStatus.EXPIRED,

    (BookingStatus.IN_PROGRESS to BookingStatusEvent.PROVIDER_RESERVED) to BookingStatus.RESERVED,
    //fast path -- Duffel can confirm immediately, skipping a hold stage
    (BookingStatus.IN_PROGRESS to BookingStatusEvent.PROVIDER_CONFIRMED) to BookingStatus.CONFIRMED,
    (BookingStatus.IN_PROGRESS to BookingStatusEvent.PROVIDER_FAILED) to BookingStatus.FAILED,
    (BookingStatus.IN_PROGRESS to BookingStatusEvent.CANCEL_REQUESTED) to BookingStatus.CANCELLATION_PENDING,
    (BookingStatus.IN_PROGRESS to BookingStatusEvent.EXPIRED) to BookingStatus.EXPIRED,

    (BookingStatus.RESERVED to BookingStatusEvent.PROVIDER_CONFIRMED  ) to BookingStatus.CONFIRMED,
    (BookingStatus.RESERVED to BookingStatusEvent.PROVIDER_FAILED  ) to BookingStatus.FAILED,
    (BookingStatus.RESERVED to BookingStatusEvent.CANCEL_REQUESTED  ) to BookingStatus.CANCELLATION_PENDING,
    (BookingStatus.RESERVED to BookingStatusEvent.EXPIRED  ) to BookingStatus.EXPIRED,

    (BookingStatus.CONFIRMED to BookingStatusEvent.CANCEL_REQUESTED  ) to BookingStatus.CANCELLATION_PENDING,
    (BookingStatus.CANCELLATION_PENDING to BookingStatusEvent.CANCEL_CONFIRMED) to BookingStatus.CANCELLED

      )


