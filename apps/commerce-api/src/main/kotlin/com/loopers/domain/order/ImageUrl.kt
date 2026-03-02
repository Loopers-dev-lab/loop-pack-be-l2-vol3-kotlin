package com.loopers.domain.order

@JvmInline
value class ImageUrl(
    val value: String
) {
    init {
        require(value.isNotBlank()) { "이미지 URL은 필수값 입니다." }
    }
}
