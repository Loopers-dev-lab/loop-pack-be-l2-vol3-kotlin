package com.loopers.domain.coupon.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class MinOrderAmount(val value: Long?) {
    init {
        if (value != null && value < 0) {
            throw CoreException(ErrorType.INVALID_MIN_ORDER_AMOUNT)
        }
    }
}
