package com.loopers.domain.catalog.brand.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class BrandName(val value: String) {
    init {
        if (value.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드명은 필수입니다.")
        }
    }
}
