package com.travel.common.model

import com.travel.common.enums.CabinClass
import com.travel.common.enums.SortOption
import com.travel.common.enums.TripType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

data class TripSlice(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter IATA code")
    val origin: String,
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter IATA code")
    val destination: String,
    @field:NotNull
    val departureDate: LocalDate,
)

data class FlightSearchRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter IATA code")
    val origin: String,
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter IATA code")
    val destination: String,
    @field:NotNull
    val departureDate: LocalDate,
    val returnDate: LocalDate? = null,
    val tripType: TripType = TripType.ONE_WAY,
    val cabinClass: CabinClass = CabinClass.ECONOMY,
    @field:Valid
    val passengers: PassengerCount = PassengerCount(),
    val currency: String = "INR",
    val maxConnections: Int? = null,
    val preferredAirlines: List<String> = emptyList(),
    val sortBy: SortOption = SortOption.BEST,
    @field:Valid
    val filters: FlightSearchFilters = FlightSearchFilters(),
    val additionalSlices: List<TripSlice> = emptyList(),
)
