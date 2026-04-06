package com.loopers.domain.ranking

import com.loopers.domain.ranking.repository.RankingScoreRepository

class FakeRankingScoreRepository : RankingScoreRepository {

    private val scores = mutableMapOf<Long, Double>()

    override fun incrementScore(productId: Long, score: Double) {
        scores[productId] = (scores[productId] ?: 0.0) + score
    }

    fun getScore(productId: Long): Double = scores[productId] ?: 0.0

    fun clear() {
        scores.clear()
    }
}
