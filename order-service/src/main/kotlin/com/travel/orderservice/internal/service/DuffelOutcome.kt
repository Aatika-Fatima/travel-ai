package com.travel.orderservice.internal.service

import com.travel.common.exception.BookingException
import com.travel.common.exception.BookingFailureReason
import com.travel.common.exception.ExternalApiException
import com.travel.common.model.BookingResult
import com.travel.orderservice.api.OrderStatusEvent

// The direct code form of the P6 status-code table -- every row there has
// exactly one corresponding branch here, and every branch corresponds to
// exactly one row.
object DuffelOutcome {
    fun confirmed(result: BookingResult): OrderStatusEvent = OrderStatusEvent.DUFFEL_2XX_FULL

    fun from(ex: Throwable): OrderStatusEvent = when {
        // 202 and ambiguous 500 land on the same event on purpose -- both
        // mean "don't retry, don't assume, let P8 resolve it from the
        // outside."
        ex is ExternalApiException && ex.statusCode == 202 -> OrderStatusEvent.DUFFEL_PENDING
        ex is ExternalApiException && ex.statusCode == 500 -> OrderStatusEvent.DUFFEL_PENDING
        ex is BookingException && ex.reason == BookingFailureReason.PAYMENT_DECLINED ->
            OrderStatusEvent.DUFFEL_PAYMENT_DECLINED
        // An expired/withdrawn offer is terminal for this order the same way
        // a validation failure is -- retrying the same offer can only fail
        // again. Same FAILED transition; the customer-facing reason on the
        // order row is what distinguishes them.
        ex is BookingException &&
            (ex.reason == BookingFailureReason.VALIDATION_FAILED || ex.reason == BookingFailureReason.OFFER_EXPIRED) ->
            OrderStatusEvent.DUFFEL_VALIDATION_FAILED
        // 503 and other retryable ExternalApiExceptions are not caught here --
        // they propagate up through @Retryable on DuffelOrderClient instead.
        else -> throw ex
    }
}
