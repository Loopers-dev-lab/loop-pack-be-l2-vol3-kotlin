package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.step.MonthlyFixedRankingItemWriter
import com.loopers.batch.job.ranking.step.PrepareMonthFixedTasklet
import com.loopers.batch.job.ranking.step.PrepareWeekFixedTasklet
import com.loopers.batch.job.ranking.step.WeeklyFixedRankingItemWriter
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import javax.sql.DataSource

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = FixedRankingJobConfig.JOB_NAME)
@Configuration
class FixedRankingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val prepareWeekFixedTasklet: PrepareWeekFixedTasklet,
    private val weeklyFixedRankingItemWriter: WeeklyFixedRankingItemWriter,
    private val prepareMonthFixedTasklet: PrepareMonthFixedTasklet,
    private val monthlyFixedRankingItemWriter: MonthlyFixedRankingItemWriter,
    private val dataSource: DataSource,
) {
    companion object {
        const val JOB_NAME = "fixedRankingMaterializeJob"
        private const val PREPARE_WEEK_FIXED_STEP = "prepareWeekFixedStep"
        private const val MATERIALIZE_WEEK_FIXED_STEP = "materializeWeekFixedStep"
        private const val PREPARE_MONTH_FIXED_STEP = "prepareMonthFixedStep"
        private const val MATERIALIZE_MONTH_FIXED_STEP = "materializeMonthFixedStep"
        private const val CHUNK_SIZE = 20
    }

    @Bean(JOB_NAME)
    fun fixedRankingMaterializeJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(prepareWeekFixedStep())
            .next(materializeWeekFixedStep())
            .next(prepareMonthFixedStep())
            .next(materializeMonthFixedStep())
            .listener(jobListener)
            .build()

    @JobScope
    @Bean(PREPARE_WEEK_FIXED_STEP)
    fun prepareWeekFixedStep(): Step =
        StepBuilder(PREPARE_WEEK_FIXED_STEP, jobRepository)
            .tasklet(prepareWeekFixedTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()

    @JobScope
    @Bean(MATERIALIZE_WEEK_FIXED_STEP)
    fun materializeWeekFixedStep(): Step =
        StepBuilder(MATERIALIZE_WEEK_FIXED_STEP, jobRepository)
            .chunk<FixedRankingAggregateRow, FixedRankingAggregateRow>(CHUNK_SIZE, transactionManager)
            .reader(weeklyFixedRankingReader(null, null))
            .writer(weeklyFixedRankingItemWriter)
            .listener(stepMonitorListener)
            .build()

    @JobScope
    @Bean(PREPARE_MONTH_FIXED_STEP)
    fun prepareMonthFixedStep(): Step =
        StepBuilder(PREPARE_MONTH_FIXED_STEP, jobRepository)
            .tasklet(prepareMonthFixedTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()

    @JobScope
    @Bean(MATERIALIZE_MONTH_FIXED_STEP)
    fun materializeMonthFixedStep(): Step =
        StepBuilder(MATERIALIZE_MONTH_FIXED_STEP, jobRepository)
            .chunk<FixedRankingAggregateRow, FixedRankingAggregateRow>(CHUNK_SIZE, transactionManager)
            .reader(monthlyFixedRankingReader(null, null))
            .writer(monthlyFixedRankingItemWriter)
            .listener(stepMonitorListener)
            .build()

    @StepScope
    @Bean
    fun weeklyFixedRankingReader(
        @Value("#{jobExecutionContext['weekPeriodStartDate']}") periodStartDate: LocalDate?,
        @Value("#{jobExecutionContext['weekPeriodEndDate']}") periodEndDate: LocalDate?,
    ): JdbcCursorItemReader<FixedRankingAggregateRow> =
        aggregatedReader("weeklyFixedRankingReader", periodStartDate, periodEndDate)

    @StepScope
    @Bean
    fun monthlyFixedRankingReader(
        @Value("#{jobExecutionContext['monthPeriodStartDate']}") periodStartDate: LocalDate?,
        @Value("#{jobExecutionContext['monthPeriodEndDate']}") periodEndDate: LocalDate?,
    ): JdbcCursorItemReader<FixedRankingAggregateRow> =
        aggregatedReader("monthlyFixedRankingReader", periodStartDate, periodEndDate)

    private fun aggregatedReader(
        name: String,
        periodStartDate: LocalDate?,
        periodEndDate: LocalDate?,
    ): JdbcCursorItemReader<FixedRankingAggregateRow> {
        if (periodStartDate == null || periodEndDate == null) {
            return JdbcCursorItemReaderBuilder<FixedRankingAggregateRow>()
                .name(name)
                .dataSource(dataSource)
                .sql(
                    """
                    SELECT
                        product_id AS productId,
                        ranking_score AS totalScore
                    FROM tb_product_metrics
                    WHERE 1 = 0
                    """.trimIndent(),
                )
                .rowMapper(DataClassRowMapper(FixedRankingAggregateRow::class.java))
                .build()
        }

        return JdbcCursorItemReaderBuilder<FixedRankingAggregateRow>()
            .name(name)
            .dataSource(dataSource)
            .fetchSize(100)
            .sql(
                """
                SELECT
                    product_id AS productId,
                    SUM(ranking_score) AS totalScore
                FROM tb_product_metrics
                WHERE metrics_date BETWEEN '$periodStartDate' AND '$periodEndDate'
                GROUP BY product_id
                ORDER BY totalScore DESC, productId ASC
                LIMIT 100
                """.trimIndent(),
            )
            .rowMapper(DataClassRowMapper(FixedRankingAggregateRow::class.java))
            .build()
    }
}
