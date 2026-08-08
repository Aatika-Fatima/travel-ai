package com.travel.common.util

object IataCodeValidator {
    private val IATA_PATTERN = Regex("^[A-Z]{3}$")

    fun isValid(code: String): Boolean = IATA_PATTERN.matches(code.uppercase())
}
