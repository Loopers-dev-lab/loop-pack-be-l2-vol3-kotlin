package com.loopers.domain.order

@JvmInline
value class DiscountAmount(val value: Long) {
    init {
        require(value >= 0) { "할인 금액은 0 이상이어야 합니다." }
    }
}
