package com.loopers.domain.common.event

data class ProductUnlikedEvent(
    val userId: Long,
    val loginId: String,
    val productId: Long,
)
