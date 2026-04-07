package com.loopers.application.event

data class ProductViewedEvent(
    val userId: Long?,
    val productId: Long,
)
