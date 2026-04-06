package com.loopers.domain.ranking.repository

interface RankingScoreRepository {
    fun incrementScore(productId: Long, score: Double)
}
