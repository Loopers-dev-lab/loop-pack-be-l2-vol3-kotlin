package com.loopers.domain.product.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class Stock(val value: Int) {
    init {
        if (value < 0) {
            throw CoreException(ErrorType.INSUFFICIENT_STOCK)
        }
    }

    fun deduct(quantity: Int): Stock {
        if (value < quantity) {
            throw CoreException(ErrorType.INSUFFICIENT_STOCK)
        }
        return Stock(value - quantity)
    }

    fun restore(quantity: Int): Stock {
        return Stock(value + quantity)
    }
}
