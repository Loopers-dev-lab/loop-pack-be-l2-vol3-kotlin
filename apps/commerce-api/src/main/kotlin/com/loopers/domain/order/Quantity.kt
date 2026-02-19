package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Quantity(val value: Int) {

    init {
        if (value < 1) {
            throw CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.")
        }
    }
}
