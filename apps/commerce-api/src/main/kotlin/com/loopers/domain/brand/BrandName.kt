package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class BrandName(val value: String) {
    init {
        if (value.isBlank() || value.length > MAX_LENGTH) {
            throw CoreException(ErrorType.BRAND_INVALID_NAME)
        }
    }

    companion object {
        private const val MAX_LENGTH = 50
    }
}
