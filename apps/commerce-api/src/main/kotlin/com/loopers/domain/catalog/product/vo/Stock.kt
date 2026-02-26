package com.loopers.domain.catalog.product.vo

import com.loopers.domain.common.vo.Quantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

@JvmInline
value class Stock(val value: Int) {
    init {
        if (value < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.")
        }
    }

    fun decrease(quantity: Quantity): Stock {
        if (value < quantity.value) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.")
        }
        return Stock(value - quantity.value)
    }

    fun increase(quantity: Quantity): Stock {
        return Stock(value + quantity.value)
    }
}
