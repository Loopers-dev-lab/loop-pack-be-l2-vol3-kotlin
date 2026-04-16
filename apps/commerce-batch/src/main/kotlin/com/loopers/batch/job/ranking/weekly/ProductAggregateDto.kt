package com.loopers.batch.job.ranking.weekly

data class ProductAggregateDto(
    val productId: Long,
    val viewCount: Long,
    val likeCount: Long,
    val salesCount: Long,
)
