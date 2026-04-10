package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingEventLog
import com.loopers.domain.ranking.RankingEventLogRepository
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.infrastructure.ranking.RankingRedisRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class RankingAggregationService(
    private val rankingRedisRepository: RankingRedisRepository,
    private val rankingScorePolicy: RankingScorePolicy,
    private val rankingWeightProvider: RankingWeightProvider,
    private val rankingEventLogRepository: RankingEventLogRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun processViewEvent(productId: Long, date: LocalDate, dateTime: LocalDateTime, eventId: String) {
        val score = rankingScorePolicy.calculateViewScore(rankingWeightProvider.getViewWeight())
        rankingRedisRepository.incrementScore(productId, score, date)
        rankingRedisRepository.incrementHourlyScore(productId, score, dateTime)
        saveEventLog(productId, "VIEW", 1.0, date, eventId)
    }

    fun processLikeEvent(productId: Long, date: LocalDate, dateTime: LocalDateTime, eventId: String) {
        val score = rankingScorePolicy.calculateLikeScore(rankingWeightProvider.getLikeWeight())
        rankingRedisRepository.incrementScore(productId, score, date)
        rankingRedisRepository.incrementHourlyScore(productId, score, dateTime)
        saveEventLog(productId, "LIKE", 1.0, date, eventId)
    }

    fun processOrderEvent(items: List<OrderItemScore>, date: LocalDate, dateTime: LocalDateTime, eventId: String) {
        val orderWeight = rankingWeightProvider.getOrderWeight()
        for (item in items) {
            val score = rankingScorePolicy.calculateOrderScore(item.amount, orderWeight)
            rankingRedisRepository.incrementScore(item.productId, score, date)
            rankingRedisRepository.incrementHourlyScore(item.productId, score, dateTime)
            saveEventLog(item.productId, "ORDER", item.amount.toDouble(), date, eventId)
        }
    }

    private fun saveEventLog(productId: Long, eventType: String, eventValue: Double, date: LocalDate, eventId: String) {
        try {
            rankingEventLogRepository.save(
                RankingEventLog(
                    productId = productId,
                    eventType = eventType,
                    eventValue = eventValue,
                    occurredDate = date,
                    eventId = eventId,
                ),
            )
        } catch (e: Exception) {
            log.warn("랭킹 이벤트 로그 저장 실패: productId={}, eventType={}", productId, eventType, e)
        }
    }
}
