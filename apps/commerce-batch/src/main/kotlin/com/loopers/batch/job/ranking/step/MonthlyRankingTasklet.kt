package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.domain.ProductRankMonthly
import com.loopers.batch.job.ranking.infrastructure.ProductRankMonthlyJpaRepository
import com.loopers.batch.job.ranking.infrastructure.RankingMetricJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus

class MonthlyRankingTasklet(
    private val rankingMetricJpaRepository: RankingMetricJpaRepository,
    private val productRankMonthlyJpaRepository: ProductRankMonthlyJpaRepository,
    private val startDate: String,
    private val endDate: String,
    private val periodDate: String,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        productRankMonthlyJpaRepository.deleteByPeriodDate(periodDate)
        log.info("[MonthlyRanking] Cleared existing data for period={}", periodDate)

        val aggregated = rankingMetricJpaRepository.findTopAggregatedByDateRange(startDate, endDate)
        val entities = aggregated.mapIndexed { index, row ->
            ProductRankMonthly(
                productId = (row[0] as Number).toLong(),
                rankingRank = index + 1,
                totalScore = (row[1] as Number).toDouble(),
                periodDate = periodDate,
            )
        }
        productRankMonthlyJpaRepository.saveAll(entities)

        log.info("[MonthlyRanking] Saved {} rankings for period={} ({} ~ {})", entities.size, periodDate, startDate, endDate)
        return RepeatStatus.FINISHED
    }
}
