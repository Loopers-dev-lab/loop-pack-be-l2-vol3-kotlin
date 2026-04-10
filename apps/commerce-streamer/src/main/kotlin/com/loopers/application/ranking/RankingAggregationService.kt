package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingEventLog
import com.loopers.domain.ranking.RankingEventLogRepository
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.domain.ranking.ViewSignals
import com.loopers.domain.ranking.ViewTrustScoreCalculator
import com.loopers.infrastructure.ranking.RankingRedisRepository
import com.loopers.infrastructure.ranking.ViewRateRedisRepository
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
    private val viewTrustScoreCalculator: ViewTrustScoreCalculator,
    private val viewRateRedisRepository: ViewRateRedisRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun processViewEvent(
        productId: Long,
        date: LocalDate,
        dateTime: LocalDateTime,
        eventId: String,
        payload: Map<String, Any?>,
    ) {
        val trustScore = calculateTrustScore(productId, payload, dateTime)
        val baseScore = rankingScorePolicy.calculateViewScore(rankingWeightProvider.getViewWeight())
        val finalScore = baseScore * trustScore

        if (finalScore > 0) {
            rankingRedisRepository.incrementScore(productId, finalScore, date)
            rankingRedisRepository.incrementHourlyScore(productId, finalScore, dateTime)
            saveEventLog(productId, "VIEW", trustScore, date, eventId)
        }
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

    private fun calculateTrustScore(productId: Long, payload: Map<String, Any?>, dateTime: LocalDateTime): Double {
        val loginId = payload["loginId"] as? String
        val clientIp = payload["clientIp"] as? String
        val userAgent = payload["userAgent"] as? String
        val referer = payload["referer"] as? String

        val identifier = loginId ?: clientIp ?: "unknown"
        val requestsPerMinute = viewRateRedisRepository.incrementAndGetRequestCount(identifier, dateTime)
        val distinctProducts = viewRateRedisRepository.addViewedProductAndGetCount(identifier, productId, dateTime)

        val signals = ViewSignals(
            isLoggedIn = loginId != null,
            hasUserAgent = !userAgent.isNullOrBlank(),
            hasReferer = !referer.isNullOrBlank(),
            requestsPerMinute = requestsPerMinute,
            distinctProductsIn10Min = distinctProducts,
        )

        return viewTrustScoreCalculator.calculate(signals)
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
