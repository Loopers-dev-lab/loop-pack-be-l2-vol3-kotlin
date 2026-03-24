package com.loopers.domain.productlike.event

import java.util.UUID

data class ProductUnlikedEvent(
    val productId: Long,
    val userId: Long,
    val dedupeKey: String = "product.unliked:$userId:$productId:${UUID.randomUUID()}",
)
