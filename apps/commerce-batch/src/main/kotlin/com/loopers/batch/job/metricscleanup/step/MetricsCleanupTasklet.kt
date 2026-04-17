package com.loopers.batch.job.metricscleanup.step

import com.loopers.batch.job.metricscleanup.MetricsCleanupJobConfig
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MetricsCleanupJobConfig.JOB_NAME)
@Component
class MetricsCleanupTasklet(
    private val jdbcTemplate: JdbcTemplate,
) : Tasklet {
    private val log = LoggerFactory.getLogger(MetricsCleanupTasklet::class.java)

    companion object {
        private const val RETENTION_DAYS = 60L
    }

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val cutoffDate = LocalDate.now().minusDays(RETENTION_DAYS)
        val deletedCount = jdbcTemplate.update(
            "DELETE FROM product_metrics WHERE date < ?",
            cutoffDate,
        )
        log.info("{}일 이전 메트릭 {}건 삭제 (기준일: {})", RETENTION_DAYS, deletedCount, cutoffDate)
        return RepeatStatus.FINISHED
    }
}
