package com.loopers.domain.product.vo

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType

@JvmInline
value class ProductName(val value: String) {
    companion object {
        private const val MAX_LENGTH = 255

        fun of(value: String): ProductName {
            if (value.length > MAX_LENGTH) {
                throw CoreException(ErrorType.BAD_REQUEST, "상품 이름은 ${MAX_LENGTH}자 이하여야 합니다.")
            }
            return ProductName(value)
        }
    }
}
