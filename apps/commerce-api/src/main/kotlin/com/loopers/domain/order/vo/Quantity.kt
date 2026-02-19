package com.loopers.domain.order.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class Quantity(val value: Int) {

    init {
        if (value < 1) {
            throw CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.")
        }
    }
}
