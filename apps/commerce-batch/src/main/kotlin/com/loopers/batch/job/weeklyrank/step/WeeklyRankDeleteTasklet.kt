package com.loopers.batch.job.weeklyrank.step

import com.loopers.batch.job.weeklyrank.WeeklyRankJobConfig
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankJobConfig.JOB_NAME)
@Component
class WeeklyRankDeleteTasklet(
    private val jdbcTemplate: JdbcTemplate,
) : Tasklet {
    private val log = LoggerFactory.getLogger(WeeklyRankDeleteTasklet::class.java)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val deletedCount = jdbcTemplate.update("DELETE FROM mv_product_rank_weekly")
        log.info("주간 랭킹 MV {}건 삭제", deletedCount)
        return RepeatStatus.FINISHED
    }
}
