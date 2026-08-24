package com.travel.bookingservice.internal.service

import com.travel.bookingservice.internal.persistence.BookingStatus

class IllegalStateTransitionException(from: BookingStatus, event: BookingStatusEvent) :
    IllegalStateException("Cannot apply $event to a booking in state $from")