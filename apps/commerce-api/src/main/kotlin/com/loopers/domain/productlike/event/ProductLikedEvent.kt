package com.loopers.domain.productlike.event

import java.util.UUID

data class ProductLikedEvent(
    val productId: Long,
    val userId: Long,
    val dedupeKey: String = "product.liked:$userId:$productId:${UUID.randomUUID()}",
)
