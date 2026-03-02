package com.loopers.domain.product

@JvmInline
value class Name(val value: String) {
    init {
        require(value.isNotBlank()) { "상품명은 필수값 입니다." }
    }
}
