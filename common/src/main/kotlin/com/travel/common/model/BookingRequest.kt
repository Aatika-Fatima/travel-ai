package com.travel.common.model

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class BookingRequest(
    @field:NotBlank
    val offerId: String,
    @field:NotEmpty
    @field:Valid
    val passengers: List<PassengerDetails>,
    @field:Valid
    val contact: ContactInfo,
    @field:NotBlank
    val paymentType: String = "balance",
)

data class PassengerDetails(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val givenName: String,
    @field:NotBlank
    val familyName: String,
    @field:NotNull
    val dateOfBirth: LocalDate,
    @field:NotBlank
    val gender: String,
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    val phoneNumber: String,
    val documents: List<TravelDocument> = emptyList(),
)

data class TravelDocument(
    val type: String,
    val uniqueIdentifier: String,
    val expiresOn: LocalDate,
    val issuingCountryCode: String,
)

data class ContactInfo(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    val phoneNumber: String,
)
