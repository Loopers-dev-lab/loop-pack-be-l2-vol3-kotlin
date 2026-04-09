package com.loopers.batch.job.productmetrics.step

import com.loopers.batch.infrastructure.catalog.ProductMetricsJdbcWriter
import com.loopers.batch.infrastructure.catalog.ProductMetricsRedisReader
import com.loopers.batch.job.productmetrics.ProductMetricsSyncJobConfig
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = ProductMetricsSyncJobConfig.JOB_NAME)
@Component
class ProductMetricsSyncTasklet(
    private val redisReader: ProductMetricsRedisReader,
    private val jdbcWriter: ProductMetricsJdbcWriter,
) : Tasklet {

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val targetDate = chunkContext.stepContext.jobParameters["requestDate"] as? LocalDate
            ?: LocalDate.now().minusDays(1)

        val metrics = redisReader.readSnapshot(targetDate)
        jdbcWriter.upsertAll(metrics)
        contribution.incrementWriteCount(metrics.size.toLong())
        return RepeatStatus.FINISHED
    }
}
