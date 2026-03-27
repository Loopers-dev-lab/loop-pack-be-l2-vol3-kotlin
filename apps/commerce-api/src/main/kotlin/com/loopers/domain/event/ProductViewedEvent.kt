package com.loopers.domain.event

import java.util.UUID

data class ProductViewedEvent(
    val productId: Long,
    val userId: Long,
    val dedupeKey: String = "product.viewed:$userId:$productId:${UUID.randomUUID()}",
)
