package com.loopers.domain.ranking

import com.loopers.domain.ranking.repository.RankingScoreRepository
import java.time.LocalDate

class FakeRankingScoreRepository : RankingScoreRepository {

    private val scores = mutableMapOf<Long, Double>()
    private val processedEventIds = mutableSetOf<String>()
    var failuresRemaining: Int = 0

    override fun incrementScore(productId: Long, score: Double, eventId: String, rankingDate: LocalDate) {
        if (failuresRemaining > 0) {
            failuresRemaining--
            throw RuntimeException("Redis 연결 실패")
        }
        if (!processedEventIds.add(eventId)) return
        scores[productId] = (scores[productId] ?: 0.0) + score
    }

    fun getScore(productId: Long): Double = scores[productId] ?: 0.0

    fun clear() {
        scores.clear()
        processedEventIds.clear()
        failuresRemaining = 0
    }
}
