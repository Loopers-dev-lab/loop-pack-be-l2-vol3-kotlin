package com.loopers.domain.coupon.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class CouponName(val value: String) {
    init {
        if (value.isBlank() || value.length > MAX_LENGTH) {
            throw CoreException(ErrorType.INVALID_COUPON_NAME)
        }
    }

    companion object {
        private const val MAX_LENGTH = 50
    }
}
