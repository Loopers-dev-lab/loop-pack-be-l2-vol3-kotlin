package com.loopers.domain.product.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

@JvmInline
value class ProductDescription(val value: String) {
    companion object {
        private const val MAX_LENGTH = 1000

        fun of(value: String): ProductDescription {
            if (value.length > MAX_LENGTH) {
                throw CoreException(ErrorType.BAD_REQUEST, "상품 설명은 ${MAX_LENGTH}자 이하여야 합니다.")
            }
            return ProductDescription(value)
        }
    }
}
