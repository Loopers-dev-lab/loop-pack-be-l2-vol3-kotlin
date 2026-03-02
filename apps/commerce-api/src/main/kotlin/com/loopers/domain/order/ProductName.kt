package com.loopers.domain.order

@JvmInline
value class ProductName(
    val value: String
) {
    init {
        require(value.isNotBlank()) { "상품명은 필수값 입니다." }
    }
}
