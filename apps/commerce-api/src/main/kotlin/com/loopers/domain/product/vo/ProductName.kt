package com.loopers.domain.product.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class ProductName(val value: String) {
    init {
        if (value.isBlank() || value.length > MAX_LENGTH) {
            throw CoreException(ErrorType.INVALID_PRODUCT_NAME_FORMAT)
        }
    }

    companion object {
        private const val MAX_LENGTH = 100
    }
}
