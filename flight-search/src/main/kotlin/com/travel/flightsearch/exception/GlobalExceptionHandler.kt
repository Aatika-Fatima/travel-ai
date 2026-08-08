package com.travel.flightsearch.exception

import com.travel.common.exception.BookingException
import com.travel.common.exception.BookingFailureReason
import com.travel.common.exception.ExternalApiException
import com.travel.common.exception.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

data class ApiErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val details: List<String> = emptyList(),
)

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Validation Failed",
                    message = ex.message ?: "Invalid search request",
                    details = ex.errors,
                ),
            )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Validation Failed",
                    message = "One or more search parameters are invalid",
                    details = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" },
                ),
            )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedRequest(ex: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Malformed Request",
                    message = "The search request body is missing required fields or is not valid JSON",
                ),
            )

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleMissingResource(ex: NoResourceFoundException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ApiErrorResponse(
                    status = HttpStatus.NOT_FOUND.value(),
                    error = "Not Found",
                    message = "No resource found at this path",
                ),
            )

    @ExceptionHandler(BookingException::class)
    fun handleBooking(ex: BookingException): ResponseEntity<ApiErrorResponse> {
        val status =
            when (ex.reason) {
                BookingFailureReason.OFFER_EXPIRED -> HttpStatus.CONFLICT
                BookingFailureReason.VALIDATION_FAILED -> HttpStatus.UNPROCESSABLE_ENTITY
                BookingFailureReason.PAYMENT_DECLINED -> HttpStatus.PAYMENT_REQUIRED
                BookingFailureReason.UNKNOWN -> HttpStatus.BAD_GATEWAY
            }
        return ResponseEntity
            .status(status)
            .body(
                ApiErrorResponse(
                    status = status.value(),
                    error = "Booking Failed",
                    message = ex.message ?: "The booking could not be completed",
                    details = ex.fieldErrors.map { (field, error) -> "$field: $error" },
                ),
            )
    }

    @ExceptionHandler(ExternalApiException::class)
    fun handleExternalApi(ex: ExternalApiException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(
                ApiErrorResponse(
                    status = HttpStatus.BAD_GATEWAY.value(),
                    error = "Upstream Search Provider Error",
                    message = ex.message ?: "The flight search provider is temporarily unavailable",
                ),
            )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    error = "Internal Server Error",
                    message = ex.message ?: "An unexpected error occurred",
                ),
            )
}
