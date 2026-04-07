package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.infrastructure.ranking.RankingRedisRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class RankingAggregationService(
    private val rankingRedisRepository: RankingRedisRepository,
    private val rankingScorePolicy: RankingScorePolicy,
) {

    fun processViewEvent(productId: Long, date: LocalDate, dateTime: LocalDateTime) {
        val score = rankingScorePolicy.calculateViewScore()
        rankingRedisRepository.incrementScore(productId, score, date)
        rankingRedisRepository.incrementHourlyScore(productId, score, dateTime)
    }

    fun processLikeEvent(productId: Long, date: LocalDate, dateTime: LocalDateTime) {
        val score = rankingScorePolicy.calculateLikeScore()
        rankingRedisRepository.incrementScore(productId, score, date)
        rankingRedisRepository.incrementHourlyScore(productId, score, dateTime)
    }

    fun processOrderEvent(items: List<OrderItemScore>, date: LocalDate, dateTime: LocalDateTime) {
        for (item in items) {
            val score = rankingScorePolicy.calculateOrderScore(item.amount)
            rankingRedisRepository.incrementScore(item.productId, score, date)
            rankingRedisRepository.incrementHourlyScore(item.productId, score, dateTime)
        }
    }
}
