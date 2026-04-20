package com.loopers.batch.job.ranking.weekly

import com.loopers.infrastructure.ranking.WeeklyProductRankJpaRepository
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
import java.time.temporal.IsoFields

/**
 * 주간 랭킹 MV 멱등성 보장용 Tasklet.
 * Chunk Step이 INSERT 하기 전에 해당 (year, week)의 기존 행을 hard DELETE 한다.
 */
@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankJobConfig.JOB_NAME)
@Component
class WeeklyRankDeleteTasklet(
    @param:Value("#{jobParameters['baseDate']}") private val baseDateStr: String,
    private val weeklyProductRankJpaRepository: WeeklyProductRankJpaRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val baseDate = LocalDate.parse(baseDateStr)
        val year = baseDate.get(IsoFields.WEEK_BASED_YEAR)
        val week = baseDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val deleted = weeklyProductRankJpaRepository.deleteByYearAndWeek(year, week)
        log.info("WeeklyRankDeleteTasklet: year={}, week={}, deleted={}", year, week, deleted)
        return RepeatStatus.FINISHED
    }
}
