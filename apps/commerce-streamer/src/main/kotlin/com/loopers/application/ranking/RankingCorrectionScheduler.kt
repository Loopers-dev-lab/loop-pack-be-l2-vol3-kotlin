package com.loopers.application.ranking

import com.loopers.common.DateUtils
import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.zset.RankingKeyGenerator
import com.loopers.zset.RedisZSetTemplate
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import kotlin.math.log10

@Component
class RankingCorrectionScheduler(
    private val productMetricsRepository: ProductMetricsRepository,
    private val redisZSetTemplate: RedisZSetTemplate,
    private val rankingProperties: RankingProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(readOnly = true)
    fun correct() {
        val today = DateUtils.todayKst()
        val correctionKey = RankingKeyGenerator.dailyKey(today) + ":correction"
        val targetKey = RankingKeyGenerator.dailyKey(today)

        try {
            val allMetrics = productMetricsRepository.findAll()
            if (allMetrics.isEmpty()) {
                log.info("보정 대상 없음")
                return
            }

            allMetrics.forEach { metrics ->
                val score = calculateScore(metrics.viewCount, metrics.likeCount, metrics.totalRevenue)
                if (score > 0) {
                    redisZSetTemplate.incrementScore(correctionKey, metrics.productId.toString(), score)
                }
            }

            redisZSetTemplate.rename(correctionKey, targetKey)
            redisZSetTemplate.setTtlIfAbsent(targetKey, Duration.ofDays(rankingProperties.ttlDays))

            log.info("랭킹 보정 완료 [date={}, 상품수={}]", today, allMetrics.size)
        } catch (e: Exception) {
            log.error("랭킹 보정 실패 [date={}]", today, e)
            try {
                redisZSetTemplate.delete(correctionKey)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun calculateScore(viewCount: Long, likeCount: Long, totalRevenue: Long): Double {
        val viewScore = rankingProperties.weight.view * viewCount
        val likeScore = rankingProperties.weight.like * likeCount
        val orderScore = if (totalRevenue > 0) rankingProperties.weight.order * log10(1.0 + totalRevenue) else 0.0
        return viewScore + likeScore + orderScore
    }
}
