package com.loopers.domain.product

@JvmInline
value class LikeCount(val value: Long) {
    init {
        require(value >= 0) { "좋아요 수는 0 이상이어야 합니다." }
    }
}
