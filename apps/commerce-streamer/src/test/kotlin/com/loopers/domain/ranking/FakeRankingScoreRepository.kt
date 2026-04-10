package com.loopers.domain.ranking

import com.loopers.domain.ranking.repository.RankingScoreRepository
import java.time.LocalDate

/**
 * 실제 RedisRankingScoreRepository는 `ranking:{YYYYMMDD}` ZSET과
 * `ranking:processed:{YYYYMMDD}` SET을 날짜별로 분리해서 관리한다.
 * Fake도 동일한 의미 모델을 유지하기 위해 날짜별 맵으로 저장한다.
 */
class FakeRankingScoreRepository : RankingScoreRepository {

    private val scoresByDate = mutableMapOf<LocalDate, MutableMap<Long, Double>>()
    private val processedEventIdsByDate = mutableMapOf<LocalDate, MutableSet<String>>()
    var failuresRemaining: Int = 0

    override fun incrementScore(productId: Long, score: Double, eventId: String, rankingDate: LocalDate) {
        if (failuresRemaining > 0) {
            failuresRemaining--
            throw RuntimeException("Redis 연결 실패")
        }
        val processed = processedEventIdsByDate.getOrPut(rankingDate) { mutableSetOf() }
        if (!processed.add(eventId)) return
        val scores = scoresByDate.getOrPut(rankingDate) { mutableMapOf() }
        scores[productId] = (scores[productId] ?: 0.0) + score
    }

    fun getScore(productId: Long, date: LocalDate): Double =
        scoresByDate[date]?.get(productId) ?: 0.0

    fun clear() {
        scoresByDate.clear()
        processedEventIdsByDate.clear()
        failuresRemaining = 0
    }
}
