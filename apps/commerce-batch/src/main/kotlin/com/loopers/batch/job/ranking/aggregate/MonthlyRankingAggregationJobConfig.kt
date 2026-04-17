package com.loopers.batch.job.ranking.aggregate

import com.loopers.batch.job.ranking.aggregate.step.ScoreProcessor
import com.loopers.batch.job.ranking.aggregate.step.SortAndAssignRankTasklet
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import javax.sql.DataSource
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class MonthlyRankingAggregationJobConfig(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val scoreProcessor: ScoreProcessor,
    private val sortAndAssignRankTasklet: SortAndAssignRankTasklet,
    private val dataSource: DataSource,
    private val transactionManager: PlatformTransactionManager,
) {
    companion object {
        const val JOB_NAME = "monthlyRankingAggregationJob"
        private const val STEP_STAGING = "monthlyStep1Staging"
        private const val STEP_RANK_ASSIGN = "monthlyStep2RankAssign"
        private const val CHUNK_SIZE = 500
    }

    @Bean(JOB_NAME)
    fun monthlyRankingAggregationJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(monthlyStep1Staging(null, null))
            .next(monthlyStep2RankAssign())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_STAGING)
    fun monthlyStep1Staging(
        @Value("#{jobParameters['startDate']}") startDate: String?,
        @Value("#{jobParameters['endDate']}") endDate: String?,
    ): Step {
        return StepBuilder(STEP_STAGING, jobRepository)
            .chunk<ProductMetricRow, ProductRankRow>(CHUNK_SIZE, transactionManager)
            .reader(monthlyMetricReader(startDate, endDate))
            .processor(scoreProcessor)
            .writer(monthlyStagingWriter(null))
            .listener(stepMonitorListener)
            .build()
    }

    @JobScope
    @Bean(STEP_RANK_ASSIGN)
    fun monthlyStep2RankAssign(): Step {
        return StepBuilder(STEP_RANK_ASSIGN, jobRepository)
            .tasklet(sortAndAssignRankTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }

    @StepScope
    @Bean("monthlyMetricReader")
    fun monthlyMetricReader(
        @Value("#{jobParameters['startDate']}") startDate: String?,
        @Value("#{jobParameters['endDate']}") endDate: String?,
    ): JdbcCursorItemReader<ProductMetricRow> {
        return JdbcCursorItemReaderBuilder<ProductMetricRow>()
            .name("monthlyMetricReader")
            .dataSource(dataSource)
            .sql(
                """
                SELECT product_id,
                       SUM(view_count)       AS view_count,
                       SUM(like_count)       AS like_count,
                       SUM(order_count)      AS order_count,
                       SUM(order_amount_sum) AS order_amount_sum
                FROM product_metrics_daily
                WHERE metric_date BETWEEN ? AND ?
                GROUP BY product_id
                ORDER BY product_id
                """.trimIndent(),
            )
            .preparedStatementSetter { ps ->
                ps.setString(1, startDate)
                ps.setString(2, endDate)
            }
            .rowMapper { rs, _ ->
                ProductMetricRow(
                    productId = rs.getLong("product_id"),
                    viewCount = rs.getLong("view_count"),
                    likeCount = rs.getLong("like_count"),
                    orderCount = rs.getLong("order_count"),
                    orderAmountSum = rs.getLong("order_amount_sum"),
                )
            }
            .build()
    }

    @StepScope
    @Bean("monthlyStagingWriter")
    fun monthlyStagingWriter(
        @Value("#{stepExecution.jobExecution.id}") jobExecutionId: Long?,
    ): JdbcBatchItemWriter<ProductRankRow> {
        return JdbcBatchItemWriterBuilder<ProductRankRow>()
            .dataSource(dataSource)
            .sql(
                """
                INSERT INTO rank_staging
                    (job_execution_id, product_id, score, view_count, like_count, order_count, order_amount_sum)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
            )
            .itemPreparedStatementSetter { item, ps ->
                ps.setLong(1, jobExecutionId ?: 0L)
                ps.setLong(2, item.productId)
                ps.setDouble(3, item.score)
                ps.setLong(4, item.viewCount)
                ps.setLong(5, item.likeCount)
                ps.setLong(6, item.orderCount)
                ps.setLong(7, item.orderAmountSum)
            }
            .build()
    }
}
