package com.loopers.domain.ranking

import com.loopers.infrastructure.ranking.RankingRedisRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class RankingAggregationService(
    private val rankingEventRepository: RankingEventRepository,
    private val rankingMetricRepository: RankingMetricRepository,
    private val rankingRedisRepository: RankingRedisRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    fun aggregate() {
        val aggregated = rankingEventRepository.aggregateUnaggregated()
        if (aggregated.isEmpty()) return

        log.info("[RankingAggregation] Aggregating {} product-date groups", aggregated.size)

        aggregated.forEach { agg ->
            val metric = rankingMetricRepository.findByProductIdAndRankingDate(agg.productId, agg.rankingDate)
                ?: RankingMetric(productId = agg.productId, rankingDate = agg.rankingDate)
            metric.addScore(agg.totalScore, agg.count)
            rankingMetricRepository.save(metric)
        }

        rankingEventRepository.markAllAggregated()

        syncToRedis(todayDate())

        rankingEventRepository.deleteAggregatedBefore(ZonedDateTime.now().minusDays(1))

        log.info("[RankingAggregation] Completed aggregation cycle")
    }

    fun syncToRedis(date: String) {
        val metrics = rankingMetricRepository.findAllByRankingDate(date)
        if (metrics.isEmpty()) return

        val scores = metrics.associate { it.productId to it.totalScore }
        rankingRedisRepository.replaceAll(date, scores)
    }

    fun rebuildRedis(date: String) = syncToRedis(date)

    private fun todayDate(): String = LocalDate.now().format(DATE_FORMAT)
}
