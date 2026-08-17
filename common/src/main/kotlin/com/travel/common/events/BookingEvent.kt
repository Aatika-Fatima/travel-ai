package com.travel.common.events

class BookingEvent(val orderId: String, val bookingReference: String, val emails: List<String>)