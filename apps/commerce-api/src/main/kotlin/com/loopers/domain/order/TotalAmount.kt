package com.loopers.domain.order

@JvmInline
value class TotalAmount(
    val value: Long
) {
    init {
        require(value > 0) { "총 주문 금액은 0보다 커야합니다." }
    }
}
