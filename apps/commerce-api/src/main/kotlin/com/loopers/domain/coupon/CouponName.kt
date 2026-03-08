package com.loopers.domain.coupon

data class CouponName(val value: String) {

    init {
        require(value.isNotBlank()) { "쿠폰명은 빈 문자열일 수 없습니다." }
        require(value.length <= MAX_LENGTH) { "쿠폰명은 ${MAX_LENGTH}자 이내여야 합니다." }
    }

    companion object {
        private const val MAX_LENGTH = 100
    }
}
