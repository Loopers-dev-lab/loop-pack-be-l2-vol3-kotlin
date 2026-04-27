package com.loopers.batch.job.ranking

data class ProductAggregateDto(
    val productId: Long,
    val viewCount: Long,
    val likeCount: Long,
    val salesCount: Long,
)
