package com.loopers.domain.product

@JvmInline
value class Stock(
    val value: Long,
) {
    init {
        require(value >= 0) { "재고는 0 이상이어야 합니다." }
    }
}
