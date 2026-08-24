package com.travel.orderservice.api

// One value per row of the transition table in OrderStatus.ALLOWED. SUBMIT
// is the only one this codebase never actually raises -- rows are inserted
// already at PENDING_SUBMISSION, not DRAFT -- kept here because it's still
// a documented, legal edge for anything that later wants to insert a DRAFT
// row ahead of submission.
enum class OrderStatusEvent {
    SUBMIT,
    DUFFEL_2XX_FULL,
    DUFFEL_PENDING,
    DUFFEL_PAYMENT_DECLINED,
    DUFFEL_VALIDATION_FAILED,
    WEBHOOK_CONFIRMED,
    RECONCILIATION_NOT_FOUND,
    TICKETED,
    CANCEL,
}
