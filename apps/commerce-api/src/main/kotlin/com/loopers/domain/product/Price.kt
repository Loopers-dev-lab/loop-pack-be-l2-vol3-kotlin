package com.loopers.domain.product

@JvmInline
value class Price(val value: Long) {
    init {
        require(value > 0) { "가격은 0보다 커야 합니다." }
    }
}
