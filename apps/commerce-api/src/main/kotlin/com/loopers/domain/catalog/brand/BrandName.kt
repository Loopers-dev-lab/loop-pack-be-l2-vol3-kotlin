package com.loopers.domain.catalog.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class BrandName(val value: String) {
    init {
        if (value.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드명은 필수입니다.")
        }
    }
}
