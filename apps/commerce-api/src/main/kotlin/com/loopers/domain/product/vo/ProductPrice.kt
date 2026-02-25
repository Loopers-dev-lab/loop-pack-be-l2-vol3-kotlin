package com.loopers.domain.product.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class ProductPrice(val value: Long) {
    init {
        if (value < 0) {
            throw CoreException(ErrorType.INVALID_PRODUCT_PRICE)
        }
    }
}
