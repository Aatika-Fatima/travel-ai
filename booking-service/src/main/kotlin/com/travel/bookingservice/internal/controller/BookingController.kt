package com.travel.bookingservice.internal.controller

import com.travel.bookingservice.internal.service.BookingService
import com.travel.bookingservice.internal.service.BookingView
import com.travel.common.model.BookingRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.uuid.Uuid

@RestController
@RequestMapping("/bookings")
class BookingController(private val bookingService: BookingService) {


    @PostMapping
    fun submit(@RequestHeader("Idempotency-Key") idempotencyKey: String,
               @RequestBody @Valid request: BookingRequest): ResponseEntity<BookingView> {
        val view = bookingService.submit(idempotencyKey, request)
        val status = if(view.justCreated) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(view)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Uuid): ResponseEntity<BookingView> =
        ResponseEntity.ok(bookingService.get(id))

    @DeleteMapping("/{id}")
    fun cancel(@PathVariable id: Uuid): ResponseEntity<BookingView> =
        ResponseEntity.ok(bookingService.cancel(id))
}