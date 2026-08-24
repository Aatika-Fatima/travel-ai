package com.travel.orderservice.api

// Copied 1:1 from booking-service's equivalent, down to the message shape --
// see IllegalStateTransitionException in booking_service.html §P4.
class IllegalStateTransitionException(from: OrderStatus, event: OrderStatusEvent) :
    IllegalStateException("Cannot apply $event to an order in state $from")
