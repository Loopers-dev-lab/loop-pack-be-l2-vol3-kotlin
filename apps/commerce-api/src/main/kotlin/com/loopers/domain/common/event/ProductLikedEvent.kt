package com.loopers.domain.common.event

data class ProductLikedEvent(
    val userId: Long,
    val loginId: String,
    val productId: Long,
    val isNewLike: Boolean,
)
