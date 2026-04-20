package com.loopers.batch.job.ranking.monthly

import com.loopers.infrastructure.ranking.MonthlyProductRankJpaRepository
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

/**
 * 월간 랭킹 MV 멱등성 보장용 Tasklet.
 * Chunk Step이 INSERT 하기 전에 해당 (year, month)의 기존 행을 hard DELETE 한다.
 */
@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MonthlyRankJobConfig.JOB_NAME)
@Component
class MonthlyRankDeleteTasklet(
    @param:Value("#{jobParameters['baseDate']}") private val baseDateStr: String,
    private val monthlyProductRankJpaRepository: MonthlyProductRankJpaRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val baseDate = LocalDate.parse(baseDateStr)
        val year = baseDate.year
        val month = baseDate.monthValue
        val deleted = monthlyProductRankJpaRepository.deleteByYearAndMonth(year, month)
        log.info("MonthlyRankDeleteTasklet: year={}, month={}, deleted={}", year, month, deleted)
        return RepeatStatus.FINISHED
    }
}
