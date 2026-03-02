package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class Quantity(val value: Int) {
    init {
        if (value < 0) {
            throw CoreException(ErrorType.INVALID_QUANTITY)
        }
    }

    fun decrease(amount: Quantity): Quantity {
        val result = value - amount.value
        if (result < 0) {
            throw CoreException(ErrorType.INVALID_QUANTITY)
        }
        return Quantity(result)
    }

    fun increase(amount: Quantity): Quantity = Quantity(value + amount.value)

    fun isEnoughFor(requested: Quantity): Boolean = value >= requested.value
}
