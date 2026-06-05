package com.loopers.batch.job.ranking.domain

data class RankingAggregation(
    val productId: Long,
    val totalScore: Double,
)
