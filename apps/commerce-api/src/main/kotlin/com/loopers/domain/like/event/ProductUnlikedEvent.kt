package com.loopers.domain.like.event

data class ProductUnlikedEvent(
    val userId: Long,
    val productId: Long,
)
