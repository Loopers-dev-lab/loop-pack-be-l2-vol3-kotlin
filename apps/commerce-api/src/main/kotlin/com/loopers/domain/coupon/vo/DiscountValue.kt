package com.loopers.domain.coupon.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class DiscountValue(val value: Long) {
    init {
        if (value <= 0) {
            throw CoreException(ErrorType.INVALID_COUPON_VALUE)
        }
    }
}
