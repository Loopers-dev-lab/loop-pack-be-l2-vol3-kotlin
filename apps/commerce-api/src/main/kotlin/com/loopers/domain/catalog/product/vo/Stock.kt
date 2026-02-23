package com.loopers.domain.catalog.product.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

@JvmInline
value class Stock(val value: Int) {
    init {
        if (value < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.")
        }
    }

    fun decrease(quantity: Int): Stock {
        if (quantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.")
        }
        if (value < quantity) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.")
        }
        return Stock(value - quantity)
    }

    fun increase(quantity: Int): Stock {
        if (quantity < 1) {
            throw CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.")
        }
        return Stock(value + quantity)
    }
}
