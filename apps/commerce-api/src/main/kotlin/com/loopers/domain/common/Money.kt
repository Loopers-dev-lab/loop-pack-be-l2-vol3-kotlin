package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal
import java.math.RoundingMode

data class Money private constructor(val amount: BigDecimal) {
    init {
        if (amount < BigDecimal.ZERO) {
            throw CoreException(ErrorType.INVALID_MONEY)
        }
    }

    fun multiply(quantity: Quantity): Money = Money(amount.multiply(BigDecimal(quantity.value)))

    fun add(other: Money): Money = Money(amount.add(other.amount))

    fun isGreaterThan(other: Money): Boolean = amount > other.amount

    companion object {
        private const val SCALE = 2

        operator fun invoke(amount: BigDecimal): Money =
            Money(amount.setScale(SCALE, RoundingMode.HALF_UP))
    }
}
