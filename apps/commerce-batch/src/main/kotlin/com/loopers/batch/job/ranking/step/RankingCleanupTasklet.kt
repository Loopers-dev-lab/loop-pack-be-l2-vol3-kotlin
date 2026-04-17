package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.ProductRankMonthlyRepository
import com.loopers.domain.ranking.ProductRankWeeklyRepository
import com.loopers.domain.ranking.RankingPeriodType
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import java.time.LocalDate

class RankingCleanupTasklet(
    private val periodType: RankingPeriodType,
    private val requestDate: LocalDate,
    private val weeklyRepository: ProductRankWeeklyRepository,
    private val monthlyRepository: ProductRankMonthlyRepository,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val periodStartDate = periodType.periodStartDate(requestDate)

        log.info("[RankingCleanup] periodType={}, periodStartDate={}", periodType, periodStartDate)

        when (periodType) {
            RankingPeriodType.WEEKLY -> weeklyRepository.deleteByPeriodStartDate(periodStartDate)
            RankingPeriodType.MONTHLY -> monthlyRepository.deleteByPeriodStartDate(periodStartDate)
        }

        return RepeatStatus.FINISHED
    }
}
