package com.travel.orderservice.internal.controller

import com.travel.orderservice.api.OrderView
import com.travel.orderservice.api.SubmitOrderRequest
import com.travel.orderservice.api.toCommand
import com.travel.orderservice.internal.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.uuid.Uuid

@RestController
@RequestMapping("/orders")
class OrderController(private val orderService: OrderService) {
    @PostMapping
    fun submit(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody @Valid request: SubmitOrderRequest,
    ): ResponseEntity<OrderView> {
        val view = orderService.submit(request.toCommand(idempotencyKey))
        // 200 vs 201 tells the client whether this call actually created
        // anything, or returned an already-existing order (UC2) — useful for
        // client-side telemetry, not required for correctness.
        val status = if (view.justCreated) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(view)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Uuid): ResponseEntity<OrderView> =
        orderService.findById(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    // Lets a caller who only ever supplied an Idempotency-Key (e.g. a
    // frontend polling after booking-service's own POST /bookings, before
    // the booking->order saga leg has produced an order id it could poll by
    // directly) discover the resulting order without needing to know
    // order-service's own generated id up front.
    @GetMapping("/by-idempotency-key/{key}")
    fun getByIdempotencyKey(@PathVariable key: String): ResponseEntity<OrderView> =
        orderService.findByIdempotencyKey(key)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
}
