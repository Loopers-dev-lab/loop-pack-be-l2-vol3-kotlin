package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.FixedRankingScope
import com.loopers.infrastructure.ranking.ProductRankMonthlyMvJpaRepository
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@StepScope
@Component
class PrepareMonthFixedTasklet(
    @param:Value("#{jobParameters['scope']}") private val scope: String?,
    @param:Value("#{jobParameters['targetDate']}") private val targetDate: String?,
    private val productRankMonthlyMvJpaRepository: ProductRankMonthlyMvJpaRepository,
) : Tasklet {
    companion object {
        private val ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
        const val PERIOD_START_KEY = "monthPeriodStartDate"
        const val PERIOD_END_KEY = "monthPeriodEndDate"
    }

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val selectedScope = FixedRankingScope.from(scope)
        if (selectedScope == FixedRankingScope.WEEK_FIXED) {
            contribution.stepExecution.jobExecution.executionContext.remove(PERIOD_START_KEY)
            contribution.stepExecution.jobExecution.executionContext.remove(PERIOD_END_KEY)
            return RepeatStatus.FINISHED
        }

        val referenceDate = targetDate?.let { LocalDate.parse(it, DATE_FORMATTER) } ?: LocalDate.now(ZONE_ID).minusMonths(1)
        val yearMonth = YearMonth.from(referenceDate)
        val periodStartDate = yearMonth.atDay(1)
        val periodEndDate = yearMonth.atEndOfMonth()

        val jobExecutionContext = contribution.stepExecution.jobExecution.executionContext
        jobExecutionContext.put(PERIOD_START_KEY, periodStartDate)
        jobExecutionContext.put(PERIOD_END_KEY, periodEndDate)

        productRankMonthlyMvJpaRepository.deleteByPeriod(periodStartDate, periodEndDate)
        return RepeatStatus.FINISHED
    }
}
