package com.loopers.domain.order

@JvmInline
value class OriginalAmount(val value: Long) {
    init {
        require(value > 0) { "쿠폰 적용 전 금액은 0보다 커야 합니다." }
    }
}
