package com.loopers.domain.order

@JvmInline
value class Price(
    val value: Long
) {
    init {
        require(value > 0) {"상품 가격은 0보다 커야합니다."}
    }
}
