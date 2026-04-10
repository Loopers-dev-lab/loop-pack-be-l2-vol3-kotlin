package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingEventLog
import com.loopers.domain.ranking.RankingEventLogRepository
import com.loopers.domain.ranking.RankingRedisOperations
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.domain.ranking.ViewDedupOperations
import com.loopers.domain.ranking.ViewRateOperations
import com.loopers.domain.ranking.ViewSignals
import com.loopers.domain.ranking.ViewTrustScoreCalculator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class RankingAggregationService(
    private val rankingRedisOperations: RankingRedisOperations,
    private val rankingScorePolicy: RankingScorePolicy,
    private val rankingWeightProvider: RankingWeightProvider,
    private val rankingEventLogRepository: RankingEventLogRepository,
    private val viewDedupOperations: ViewDedupOperations,
    private val viewRateOperations: ViewRateOperations,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val viewTrustScoreCalculator = ViewTrustScoreCalculator()

    fun processViewEvent(
        productId: Long,
        date: LocalDate,
        dateTime: LocalDateTime,
        eventId: String,
        context: ViewEventContext,
    ): Boolean {
        if (viewDedupOperations.isDuplicate(productId, context.loginId, context.clientIp, date)) {
            log.debug("조회 중복 필터링: productId={}, loginId={}, ip={}", productId, context.loginId, context.clientIp)
            return false
        }

        val trustScore = calculateTrustScore(productId, context, dateTime)
        val baseScore = rankingScorePolicy.calculateViewScore(rankingWeightProvider.getViewWeight())
        val finalScore = baseScore * trustScore

        if (finalScore > 0) {
            rankingRedisOperations.incrementScore(productId, finalScore, date)
            rankingRedisOperations.incrementHourlyScore(productId, finalScore, dateTime)
            saveEventLog(productId, "VIEW", trustScore, date, eventId)
        }

        viewDedupOperations.markViewed(productId, context.loginId, context.clientIp, date)
        return true
    }

    fun processLikeEvent(productId: Long, date: LocalDate, dateTime: LocalDateTime, eventId: String) {
        val score = rankingScorePolicy.calculateLikeScore(rankingWeightProvider.getLikeWeight())
        rankingRedisOperations.incrementScore(productId, score, date)
        rankingRedisOperations.incrementHourlyScore(productId, score, dateTime)
        saveEventLog(productId, "LIKE", 1.0, date, eventId)
    }

    fun processOrderEvent(items: List<OrderItemScore>, date: LocalDate, dateTime: LocalDateTime, eventId: String) {
        val orderWeight = rankingWeightProvider.getOrderWeight()
        for (item in items) {
            val score = rankingScorePolicy.calculateOrderScore(item.amount, orderWeight)
            rankingRedisOperations.incrementScore(item.productId, score, date)
            rankingRedisOperations.incrementHourlyScore(item.productId, score, dateTime)
            saveEventLog(item.productId, "ORDER", item.amount.toDouble(), date, eventId)
        }
    }

    private fun calculateTrustScore(productId: Long, context: ViewEventContext, dateTime: LocalDateTime): Double {
        val identifier = context.loginId ?: context.clientIp ?: "unknown"
        val requestsPerMinute = viewRateOperations.incrementAndGetRequestCount(identifier, dateTime)
        val distinctProducts = viewRateOperations.addViewedProductAndGetCount(identifier, productId, dateTime)

        val signals = ViewSignals(
            isLoggedIn = context.loginId != null,
            hasUserAgent = !context.userAgent.isNullOrBlank(),
            hasReferer = !context.referer.isNullOrBlank(),
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
