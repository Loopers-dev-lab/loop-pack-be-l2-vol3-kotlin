package com.loopers.domain.catalog.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal

class Price(val value: BigDecimal) {
    init {
        if (value < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다.")
        }
    }
}
