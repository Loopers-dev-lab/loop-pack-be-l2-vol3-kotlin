package com.loopers.domain.productlike.event

data class ProductUnlikedEvent(
    val productId: Long,
    val userId: Long,
    val dedupeKey: String = "product.unliked:$userId:$productId",
)
