package com.loopers.event.payload

data class ProductLikedPayload(
    val userId: Long,
    val productId: Long,
)

data class ProductUnlikedPayload(
    val userId: Long,
    val productId: Long,
)

data class ProductViewedPayload(
    val productId: Long,
)
