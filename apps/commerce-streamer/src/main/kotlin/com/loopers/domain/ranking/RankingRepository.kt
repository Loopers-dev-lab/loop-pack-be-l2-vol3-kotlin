package com.loopers.domain.ranking

interface RankingRepository {
    fun incrementScore(key: String, productId: Long, score: Double)
}
