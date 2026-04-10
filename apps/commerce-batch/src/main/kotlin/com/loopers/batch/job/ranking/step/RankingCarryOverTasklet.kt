package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.RankingCarryOverJobConfig
import com.loopers.batch.job.ranking.domain.RankingMetric
import com.loopers.batch.job.ranking.infrastructure.RankingMetricJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = RankingCarryOverJobConfig.JOB_NAME)
@Component
class RankingCarryOverTasklet(
    private val rankingMetricJpaRepository: RankingMetricJpaRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private const val DECAY_FACTOR = 0.1
    }

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val today = LocalDate.now().format(DATE_FORMAT)
        val tomorrow = LocalDate.now().plusDays(1).format(DATE_FORMAT)

        if (rankingMetricJpaRepository.existsByRankingDate(tomorrow)) {
            log.info("[Ranking] Tomorrow's metrics already exist ({}), skipping carry-over", tomorrow)
            return RepeatStatus.FINISHED
        }

        val todayMetrics = rankingMetricJpaRepository.findAllByRankingDate(today)
        if (todayMetrics.isEmpty()) {
            log.info("[Ranking] No metrics found for today ({}), skipping carry-over", today)
            return RepeatStatus.FINISHED
        }

        val tomorrowMetrics = todayMetrics.map { metric ->
            RankingMetric(
                productId = metric.productId,
                rankingDate = tomorrow,
                totalScore = metric.totalScore * DECAY_FACTOR,
                eventCount = 0,
            )
        }
        rankingMetricJpaRepository.saveAll(tomorrowMetrics)

        log.info(
            "[Ranking] Carried over {} products from {} to {} with decay={}",
            tomorrowMetrics.size,
            today,
            tomorrow,
            DECAY_FACTOR,
        )
        return RepeatStatus.FINISHED
    }
}
