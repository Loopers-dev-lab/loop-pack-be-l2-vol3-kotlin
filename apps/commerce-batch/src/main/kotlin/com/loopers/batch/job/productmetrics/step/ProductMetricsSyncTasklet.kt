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

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = ProductMetricsSyncJobConfig.JOB_NAME)
@Component
class ProductMetricsSyncTasklet(
    private val redisReader: ProductMetricsRedisReader,
    private val jdbcWriter: ProductMetricsJdbcWriter,
) : Tasklet {

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val metrics = redisReader.readAllAndReset()
        jdbcWriter.upsertAll(metrics)
        contribution.incrementWriteCount(metrics.size.toLong())
        return RepeatStatus.FINISHED
    }
}
