package com.loopers.batch.job.ranking.weekly

import com.loopers.batch.job.ranking.AggregatedProductMetric
import com.loopers.batch.job.ranking.AggregatedProductMetricRowMapper
import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import com.loopers.infrastructure.ranking.WeeklyProductRankEntity
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.JpaItemWriter
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import javax.sql.DataSource

/**
 * 주간 랭킹 Job.
 *
 * Step 1 (Tasklet): 해당 (year, week)의 기존 MV 행을 hard delete. 멱등성 보장.
 *   - `allowStartIfComplete(true)`로 재시작 시 다시 실행되어 delete → insert 흐름의 원자성 유지.
 * Step 2 (Chunk): JdbcCursorItemReader가 SQL GROUP BY 집계 + `ROW_NUMBER() OVER (ORDER BY total_score DESC, product_id ASC)`로
 *   rank까지 산출하여 LIMIT 100으로 잘라 읽는다. Processor는 집계 결과를 MV 엔티티로 매핑만 하고,
 *   JpaItemWriter가 `mv_product_rank_weekly`에 INSERT. rank 산출을 SQL에 위임해 restart/병렬 실행에 안전하다.
 */
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankJobConfig.JOB_NAME)
@Configuration
class WeeklyRankJobConfig(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val transactionManager: PlatformTransactionManager,
    private val weeklyRankDeleteTasklet: WeeklyRankDeleteTasklet,
    private val weeklyRankProcessor: WeeklyRankProcessor,
) {
    companion object {
        const val JOB_NAME = "weeklyRankJob"
        const val DELETE_STEP_NAME = "weeklyRankDeleteStep"
        const val CHUNK_STEP_NAME = "weeklyRankStep"
        private const val CHUNK_SIZE = 100

        val AGGREGATION_SQL =
            """
            SELECT
                agg.product_id AS product_id,
                agg.view_count AS view_count,
                agg.like_count AS like_count,
                agg.units_sold AS units_sold,
                agg.sales_amount AS sales_amount,
                agg.order_score AS order_score,
                agg.total_score AS total_score,
                ROW_NUMBER() OVER (ORDER BY agg.total_score DESC, agg.product_id ASC) AS rank_number
            FROM (
                SELECT
                    pmd.product_id AS product_id,
                    SUM(pmd.view_count) AS view_count,
                    SUM(pmd.like_count) AS like_count,
                    SUM(pmd.units_sold) AS units_sold,
                    SUM(pmd.sales_amount) AS sales_amount,
                    SUM(pmd.order_score) AS order_score,
                    (SUM(pmd.view_count) * 0.1
                        + SUM(pmd.like_count) * 0.2
                        + SUM(pmd.order_score)) AS total_score
                FROM product_metrics_daily pmd
                WHERE pmd.metric_date BETWEEN ? AND ?
                    AND pmd.deleted_at IS NULL
                GROUP BY pmd.product_id
            ) agg
            ORDER BY rank_number
            LIMIT 100
            """.trimIndent()
    }

    @Bean(JOB_NAME)
    fun weeklyRankJob(
        @Qualifier(DELETE_STEP_NAME) deleteStep: Step,
        @Qualifier(CHUNK_STEP_NAME) chunkStep: Step,
    ): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(deleteStep)
            .next(chunkStep)
            .listener(jobListener)
            .build()

    @Bean(DELETE_STEP_NAME)
    fun weeklyRankDeleteStep(): Step =
        StepBuilder(DELETE_STEP_NAME, jobRepository)
            .tasklet(weeklyRankDeleteTasklet, transactionManager)
            .allowStartIfComplete(true)
            .listener(stepMonitorListener)
            .build()

    @Bean(CHUNK_STEP_NAME)
    fun weeklyRankStep(
        reader: JdbcCursorItemReader<AggregatedProductMetric>,
        writer: JpaItemWriter<WeeklyProductRankEntity>,
    ): Step =
        StepBuilder(CHUNK_STEP_NAME, jobRepository)
            .chunk<AggregatedProductMetric, WeeklyProductRankEntity>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(weeklyRankProcessor)
            .writer(writer)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()

    @StepScope
    @Bean
    fun weeklyRankReader(
        dataSource: DataSource,
        @Value("#{jobParameters['baseDate']}") baseDateStr: String,
    ): JdbcCursorItemReader<AggregatedProductMetric> {
        val baseDate = LocalDate.parse(baseDateStr)
        val startDate = baseDate.minusDays(6)
        return JdbcCursorItemReaderBuilder<AggregatedProductMetric>()
            .name("weeklyRankReader")
            .dataSource(dataSource)
            .sql(AGGREGATION_SQL)
            .preparedStatementSetter { ps ->
                ps.setObject(1, startDate)
                ps.setObject(2, baseDate)
            }
            .rowMapper(AggregatedProductMetricRowMapper())
            .build()
    }

    @Bean
    fun weeklyRankWriter(
        entityManagerFactory: EntityManagerFactory,
    ): JpaItemWriter<WeeklyProductRankEntity> =
        JpaItemWriterBuilder<WeeklyProductRankEntity>()
            .entityManagerFactory(entityManagerFactory)
            .build()
}
