package com.travel.orderservice.internal.kafka

import com.travel.orderservice.internal.service.OrderMetrics
import com.travel.orderservice.internal.service.OrderService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class BookingSagaConsumer(
    private val orderService: OrderService,
    private val metrics: OrderMetrics,
) {
    private val mapper = jacksonObjectMapper()

    // Payload shape matches booking-service's BookingCreatedPayload exactly
    // (booking_service.html §P3) -- bookingId, idempotencyKey, offerId, and
    // the full BookingRequest, so no field-by-field translation is needed
    // beyond wrapping it as a SubmitOrderCommand.
    @KafkaListener(
        topics = ["booking.events"],
        groupId = "order-service",
        containerFactory = "orderManualAckContainerFactory",
    )
    fun onBookingCreated(payload: String, ack: Acknowledgment) {
        val event = mapper.readValue(payload, BookingCreatedPayload::class.java)
        // Same idempotency key booking-service already de-duplicated on --
        // reusing it, rather than deriving a new one, is what makes THIS
        // service's uq_orders_idempotency_key line up with booking-
        // service's uq_bookings_idempotency_key for the same logical
        // request, end to end across both services.
        orderService.submit(event.toSubmitOrderCommand())
        ack.acknowledge()
    }
}