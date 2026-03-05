package com.loopers.domain.brand.vo

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType

@JvmInline
value class BrandName(val value: String) {
    companion object {
        private const val MAX_LENGTH = 255

        fun of(value: String): BrandName {
            if (value.length > MAX_LENGTH) {
                throw CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 ${MAX_LENGTH}자 이하여야 합니다.")
            }
            return BrandName(value)
        }
    }
}
