package com.loopers.domain.coupon

import com.loopers.domain.DomainException

class CouponException(
    val error: CouponError,
    message: String,
) : DomainException(message)

enum class CouponError {
    EXPIRED,
    MAX_ISSUED,
    ALREADY_ISSUED,
    NOT_AVAILABLE,
    NOT_OWNED,
    MIN_ORDER_AMOUNT,
    DELETED,
}
