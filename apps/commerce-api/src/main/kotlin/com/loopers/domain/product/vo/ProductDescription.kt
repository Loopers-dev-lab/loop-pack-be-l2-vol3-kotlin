package com.loopers.domain.product.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class ProductDescription(val value: String) {
    init {
        if (value.length > MAX_LENGTH) {
            throw CoreException(ErrorType.INVALID_PRODUCT_DESCRIPTION_LENGTH)
        }
    }

    companion object {
        private const val MAX_LENGTH = 1000
    }
}
