package com.loopers.application.event

data class ProductLikeChangedEvent(
    val productId: Long,
    val brandId: Long,
    val delta: Long,
)
