package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import jakarta.persistence.Embeddable

@Embeddable
data class Stock(
    val quantity: Int = 0,
) {
    init {
        if (quantity < 0) {
            throw CoreException(ProductErrorCode.INVALID_STOCK)
        }
    }

    fun deduct(amount: Int): Stock {
        if (quantity < amount) {
            throw CoreException(ProductErrorCode.INSUFFICIENT_STOCK)
        }
        return Stock(quantity - amount)
    }
}
