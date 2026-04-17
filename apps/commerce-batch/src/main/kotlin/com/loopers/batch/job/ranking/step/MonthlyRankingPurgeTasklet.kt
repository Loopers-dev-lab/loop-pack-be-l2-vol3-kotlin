package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.MonthlyRankingAggregationJobConfig
import com.loopers.domain.mv.MonthlyPeriod
import com.loopers.infrastructure.mv.MonthlyProductRankJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 월간 랭킹 집계 Job 의 첫 번째 Step.
 *
 * 같은 `requestDate` 로 재실행되어도 결과가 동일하도록, aggregate 이전에 해당 월의 MV 행을 모두 삭제한다.
 */
@StepScope
@ConditionalOnProperty(
    name = ["spring.batch.job.name"],
    havingValue = MonthlyRankingAggregationJobConfig.JOB_NAME,
)
@Component
class MonthlyRankingPurgeTasklet(
    private val monthlyProductRankJpaRepository: MonthlyProductRankJpaRepository,
    @param:Value("#{jobParameters['requestDate']}") private val requestDate: String,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val period = MonthlyPeriod.of(LocalDate.parse(requestDate, DateTimeFormatter.BASIC_ISO_DATE))
        val deleted = monthlyProductRankJpaRepository.deleteByYearMonthVal(period.yearMonthVal)
        log.info(
            "monthly_ranking_purge yearMonth={} periodStart={} periodEnd={} deletedRows={}",
            period.yearMonthVal,
            period.start,
            period.end,
            deleted,
        )
        return RepeatStatus.FINISHED
    }
}
