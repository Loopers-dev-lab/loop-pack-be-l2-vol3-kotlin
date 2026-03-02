package com.loopers.domain.order

@JvmInline
value class ProductDescription(
    val value: String
) {
    init {
        require(value.isNotBlank()) { "상품 정보는 필수값 입니다." }
    }
}
