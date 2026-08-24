package com.travel.bookingservice.internal.service

import kotlin.uuid.Uuid

class BookingNotFoundException(bookingId: Uuid) : NoSuchElementException("No booking found for id $bookingId")
