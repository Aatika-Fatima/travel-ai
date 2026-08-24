package com.travel.orderservice.api

// See P5 for the allow-list this enum is deliberately not allowed to
// bypass -- nothing outside OrderStatus.transition() may set this field.
enum class OrderStatus {
    DRAFT, PENDING_SUBMISSION, AWAITING_AIRLINE_CONFIRMATION,
    CONFIRMED, TICKETED, PAYMENT_FAILED, FAILED, CANCELLED;

    fun transition(event: OrderStatusEvent): OrderStatus =
        ALLOWED[this to event] ?: throw IllegalStateTransitionException(this, event)

    companion object {
        // Not private -- OrderStatusTest enumerates this directly for an
        // exhaustive cross-product check, same convention as booking-
        // service's BookingStatusEvent.ALLOWED.
        val ALLOWED = mapOf(
            (DRAFT to OrderStatusEvent.SUBMIT) to PENDING_SUBMISSION,
            (PENDING_SUBMISSION to OrderStatusEvent.DUFFEL_2XX_FULL) to CONFIRMED,
            (PENDING_SUBMISSION to OrderStatusEvent.DUFFEL_PENDING) to AWAITING_AIRLINE_CONFIRMATION,
            (PENDING_SUBMISSION to OrderStatusEvent.DUFFEL_PAYMENT_DECLINED) to PAYMENT_FAILED,
            (PENDING_SUBMISSION to OrderStatusEvent.DUFFEL_VALIDATION_FAILED) to FAILED,
            (AWAITING_AIRLINE_CONFIRMATION to OrderStatusEvent.WEBHOOK_CONFIRMED) to CONFIRMED,
            (AWAITING_AIRLINE_CONFIRMATION to OrderStatusEvent.RECONCILIATION_NOT_FOUND) to FAILED,
            (CONFIRMED to OrderStatusEvent.TICKETED) to TICKETED,
            // UC4 -- cancellation is legal from either post-booking state
            (CONFIRMED to OrderStatusEvent.CANCEL) to CANCELLED,
            (TICKETED to OrderStatusEvent.CANCEL) to CANCELLED,
        )
    }
}
