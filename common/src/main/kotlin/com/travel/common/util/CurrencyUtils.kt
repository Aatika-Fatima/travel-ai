package com.travel.common.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale

object CurrencyUtils {
    fun format(
        amount: Double,
        currencyCode: String,
    ): String {
        val currency = Currency.getInstance(currencyCode.uppercase())
        val rounded = BigDecimal.valueOf(amount).setScale(currency.defaultFractionDigits, RoundingMode.HALF_UP)
        return "${currency.getSymbol(Locale.US)}$rounded"
    }

    fun isSupported(currencyCode: String): Boolean =
        try {
            Currency.getInstance(currencyCode.uppercase())
            true
        } catch (ex: IllegalArgumentException) {
            false
        }
}
