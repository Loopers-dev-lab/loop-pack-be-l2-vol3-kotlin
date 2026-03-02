package com.loopers.domain.brand

@JvmInline
value class LogoImageUrl(val value: String) {
    init {
        require(value.isNotBlank()) { "LogoImageUrl은 필수값 입니다." }
    }
}
