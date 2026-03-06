package com.loopers.domain.coupon

@JvmInline
value class CouponName(val value: String) {
    init {
        require(value.isNotBlank()) { "쿠폰 이름은 공백일 수 없습니다." }
        require(value.length <= 100) { "쿠폰 이름은 100자 이하여야 합니다." }
    }
}
