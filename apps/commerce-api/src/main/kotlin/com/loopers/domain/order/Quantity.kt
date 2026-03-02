package com.loopers.domain.order

@JvmInline
value class Quantity(
    val value: Long
) {
    init {
        require(value >= 1) {"주문 수량은 1 이상이어야 합니다."}
    }
}
