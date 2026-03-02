package com.loopers.domain.product

@JvmInline
value class Description(val value: String) {
    init {
        require(value.isNotBlank()) { "상품 설명은 필수값 입니다." }
    }
}
