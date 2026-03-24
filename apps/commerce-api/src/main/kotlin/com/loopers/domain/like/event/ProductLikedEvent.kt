package com.loopers.domain.like.event

data class ProductLikedEvent(
    val userId: Long,
    val productId: Long,
)
