package com.travel.common.events

class EmailEvent(val emailId: String, val template: String, val recipient: String, val bookingReference: String)