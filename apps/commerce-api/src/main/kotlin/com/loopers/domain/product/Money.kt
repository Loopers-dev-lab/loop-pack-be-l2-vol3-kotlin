package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import jakarta.persistence.Embeddable

@Embeddable
data class Money(
    val amount: Long = 0,
) {
    init {
        if (amount < 0) {
            throw CoreException(ProductErrorCode.INVALID_PRICE)
        }
    }
}
