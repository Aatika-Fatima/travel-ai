package com.travel.common.model

import com.travel.common.enums.PassengerType

data class Passenger(
    val type: PassengerType,
    val firstName: String? = null,
    val lastName: String? = null,
)

data class PassengerCount(
    val adults: Int = 1,
    val children: Int = 0,
    val infants: Int = 0,
) {
    val total: Int get() = adults + children + infants
}
