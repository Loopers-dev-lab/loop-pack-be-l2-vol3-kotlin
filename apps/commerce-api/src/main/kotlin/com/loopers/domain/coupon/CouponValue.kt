package com.loopers.domain.coupon

@JvmInline
value class CouponValue(val value: Long) {
    init {
        require(value > 0) { "쿠폰 할인값은 0보다 커야 합니다." }
    }
}
