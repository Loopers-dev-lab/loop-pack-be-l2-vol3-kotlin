package com.loopers.domain.brand

@JvmInline
value class Description(val value: String) {
    init {
        require(value.isNotBlank()) { "Description은 필수값 입니다." }
    }
}
