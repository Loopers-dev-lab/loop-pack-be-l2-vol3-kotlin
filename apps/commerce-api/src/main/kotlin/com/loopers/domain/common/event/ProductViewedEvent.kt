package com.loopers.domain.common.event

data class ProductViewedEvent(
    val userId: Long,
    val loginId: String,
    val productId: Long,
)
