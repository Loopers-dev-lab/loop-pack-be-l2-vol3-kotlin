package com.loopers.batch.job.ranking

data class FixedRankingAggregateRow(
    val productId: Long,
    val totalScore: Double,
)
