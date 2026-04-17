package com.loopers.batch.job.ranking

import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import com.loopers.infrastructure.ranking.RankingPeriodDateRange
import com.loopers.infrastructure.ranking.RankingPeriodDateRangeResolver
import com.loopers.infrastructure.ranking.RankingScoreFormula
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.sql.Date
import javax.sql.DataSource

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = ProductRankingAggregationJobConfig.JOB_NAME)
@Configuration
class ProductRankingAggregationJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val dataSource: DataSource,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val jobParameterValidator: ProductRankingAggregationJobParameterValidator,
    private val weeklyProductRankingItemWriter: WeeklyProductRankingItemWriter,
    private val monthlyProductRankingItemWriter: MonthlyProductRankingItemWriter,
) {
    companion object {
        const val JOB_NAME = "productRankingAggregationJob"
        const val TARGET_DATE_PARAMETER = "targetDate"
        private const val WEEKLY_STEP_NAME = "weeklyProductRankingMaterializeStep"
        private const val MONTHLY_STEP_NAME = "monthlyProductRankingMaterializeStep"
        private const val WEEKLY_READER_NAME = "weeklyProductRankingReader"
        private const val MONTHLY_READER_NAME = "monthlyProductRankingReader"
        private const val CHUNK_SIZE = 25
        private const val TOP_N = 100
    }

    @Bean(JOB_NAME)
    fun productRankingAggregationJob(
        @Qualifier(WEEKLY_STEP_NAME) weeklyStep: Step,
        @Qualifier(MONTHLY_STEP_NAME) monthlyStep: Step,
    ): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(jobParameterValidator)
            .start(weeklyStep)
            .next(monthlyStep)
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(WEEKLY_STEP_NAME)
    fun weeklyProductRankingMaterializeStep(
        @Qualifier(WEEKLY_READER_NAME) reader: JdbcCursorItemReader<AggregatedProductRankingRow>,
        passThroughProcessor: ItemProcessor<AggregatedProductRankingRow, AggregatedProductRankingRow>,
    ): Step {
        return StepBuilder(WEEKLY_STEP_NAME, jobRepository)
            .chunk<AggregatedProductRankingRow, AggregatedProductRankingRow>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(passThroughProcessor)
            .writer(weeklyProductRankingItemWriter)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .listener(weeklyProductRankingItemWriter)
            .build()
    }

    @JobScope
    @Bean(MONTHLY_STEP_NAME)
    fun monthlyProductRankingMaterializeStep(
        @Qualifier(MONTHLY_READER_NAME) reader: JdbcCursorItemReader<AggregatedProductRankingRow>,
        passThroughProcessor: ItemProcessor<AggregatedProductRankingRow, AggregatedProductRankingRow>,
    ): Step {
        return StepBuilder(MONTHLY_STEP_NAME, jobRepository)
            .chunk<AggregatedProductRankingRow, AggregatedProductRankingRow>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(passThroughProcessor)
            .writer(monthlyProductRankingItemWriter)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .listener(monthlyProductRankingItemWriter)
            .build()
    }

    @StepScope
    @Bean(WEEKLY_READER_NAME)
    fun weeklyProductRankingReader(
        @Value("#{jobParameters['targetDate']}") targetDate: String? = null,
    ): JdbcCursorItemReader<AggregatedProductRankingRow> {
        return aggregateReader(
            readerName = WEEKLY_READER_NAME,
            dateRange = RankingPeriodDateRangeResolver.weekly(
                requireNotNull(targetDate) { "targetDate job parameter is required" },
            ),
        )
    }

    @StepScope
    @Bean(MONTHLY_READER_NAME)
    fun monthlyProductRankingReader(
        @Value("#{jobParameters['targetDate']}") targetDate: String? = null,
    ): JdbcCursorItemReader<AggregatedProductRankingRow> {
        return aggregateReader(
            readerName = MONTHLY_READER_NAME,
            dateRange = RankingPeriodDateRangeResolver.monthly(
                requireNotNull(targetDate) { "targetDate job parameter is required" },
            ),
        )
    }

    @Bean
    fun passThroughProcessor(): ItemProcessor<AggregatedProductRankingRow, AggregatedProductRankingRow> {
        return ItemProcessor { item -> item }
    }

    private fun aggregateReader(
        readerName: String,
        dateRange: RankingPeriodDateRange,
    ): JdbcCursorItemReader<AggregatedProductRankingRow> {
        return JdbcCursorItemReaderBuilder<AggregatedProductRankingRow>()
            .name(readerName)
            .dataSource(dataSource)
            .sql(
                """
                SELECT
                    product_id,
                    SUM(like_count) AS like_count,
                    SUM(view_count) AS view_count,
                    SUM(sales_count) AS sales_count,
                    (SUM(view_count) * ${RankingScoreFormula.VIEW_WEIGHT}) + (SUM(like_count) * ${RankingScoreFormula.LIKE_WEIGHT}) + (SUM(sales_count) * ${RankingScoreFormula.SALES_WEIGHT}) AS score
                FROM product_metrics
                WHERE metric_date BETWEEN ? AND ?
                GROUP BY product_id
                HAVING score > 0
                ORDER BY score DESC, product_id ASC
                LIMIT $TOP_N
                """.trimIndent(),
            )
            .preparedStatementSetter { ps ->
                ps.setDate(1, Date.valueOf(dateRange.startDate))
                ps.setDate(2, Date.valueOf(dateRange.endDate))
            }
            .rowMapper { rs, _ ->
                AggregatedProductRankingRow(
                    productId = rs.getLong("product_id"),
                    likeCount = rs.getLong("like_count"),
                    viewCount = rs.getLong("view_count"),
                    salesCount = rs.getLong("sales_count"),
                    score = rs.getDouble("score"),
                )
            }
            .build()
    }
}
