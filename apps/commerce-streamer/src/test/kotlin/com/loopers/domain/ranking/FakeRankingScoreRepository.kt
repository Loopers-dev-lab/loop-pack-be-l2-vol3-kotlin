package com.loopers.domain.ranking

import com.loopers.domain.ranking.repository.RankingScoreRepository

class FakeRankingScoreRepository : RankingScoreRepository {

    private val scores = mutableMapOf<Long, Double>()
    var failuresRemaining: Int = 0

    override fun incrementScore(productId: Long, score: Double) {
        if (failuresRemaining > 0) {
            failuresRemaining--
            throw RuntimeException("Redis 연결 실패")
        }
        scores[productId] = (scores[productId] ?: 0.0) + score
    }

    fun getScore(productId: Long): Double = scores[productId] ?: 0.0

    fun clear() {
        scores.clear()
        failuresRemaining = 0
    }
}
