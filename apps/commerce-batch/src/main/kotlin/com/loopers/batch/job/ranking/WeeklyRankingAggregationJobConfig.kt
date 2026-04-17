package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.step.WeeklyRankingProcessor
import com.loopers.batch.job.ranking.step.WeeklyRankingPurgeTasklet
import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import com.loopers.domain.mv.WeeklyPeriod
import com.loopers.domain.mv.WeeklyProductRankModel
import com.loopers.domain.ranking.BatchRankingScorePolicy
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
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
import java.sql.Date
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

/**
 * 주간 랭킹 집계 Job.
 *
 * 실행: `--job.name=weeklyRankingAggregationJob --requestDate=yyyyMMdd`
 *
 * - Step 1 (Tasklet): `requestDate` 의 ISO 주 월요일 기준으로 기존 MV 행을 삭제 (멱등 보장)
 * - Step 2 (Chunk-Oriented): `product_metrics_daily` 7일치를 SQL 에서 미리 집계·정렬·상위 100 컷팅한 뒤
 *   rank_position 을 부여하며 `mv_product_rank_weekly` 에 INSERT
 */
@ConditionalOnProperty(
    name = ["spring.batch.job.name"],
    havingValue = WeeklyRankingAggregationJobConfig.JOB_NAME,
)
@Configuration
class WeeklyRankingAggregationJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val dataSource: DataSource,
    private val entityManagerFactory: EntityManagerFactory,
    private val weeklyRankingPurgeTasklet: WeeklyRankingPurgeTasklet,
    private val weeklyRankingProcessor: WeeklyRankingProcessor,
) {
    companion object {
        const val JOB_NAME = "weeklyRankingAggregationJob"
        private const val PURGE_STEP = "weeklyRankingPurgeStep"
        private const val AGGREGATE_STEP = "weeklyRankingAggregateStep"
        private const val CHUNK_SIZE = 50
        private const val TOP_N = 100
        private const val READER_NAME = "weeklyRankingItemReader"
    }

    @Bean(JOB_NAME)
    fun weeklyRankingAggregationJob(
        @Qualifier(PURGE_STEP) purgeStep: Step,
        @Qualifier(AGGREGATE_STEP) aggregateStep: Step,
    ): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(purgeStep)
            .next(aggregateStep)
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(PURGE_STEP)
    fun weeklyRankingPurgeStep(): Step {
        return StepBuilder(PURGE_STEP, jobRepository)
            .tasklet(weeklyRankingPurgeTasklet, transactionManager)
            .allowStartIfComplete(true)
            .listener(stepMonitorListener)
            .build()
    }

    @JobScope
    @Bean(AGGREGATE_STEP)
    fun weeklyRankingAggregateStep(
        weeklyRankingItemReader: JdbcCursorItemReader<WeeklyAggregationRow>,
        weeklyRankingItemWriter: JpaItemWriter<WeeklyProductRankModel>,
    ): Step {
        return StepBuilder(AGGREGATE_STEP, jobRepository)
            .chunk<WeeklyAggregationRow, WeeklyProductRankModel>(CHUNK_SIZE, transactionManager)
            .reader(weeklyRankingItemReader)
            .processor(weeklyRankingProcessor)
            .writer(weeklyRankingItemWriter)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()
    }

    @StepScope
    @Bean
    fun weeklyRankingItemReader(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        scorePolicy: BatchRankingScorePolicy,
    ): JdbcCursorItemReader<WeeklyAggregationRow> {
        val period = WeeklyPeriod.of(LocalDate.parse(requestDate, DateTimeFormatter.BASIC_ISO_DATE))
        return JdbcCursorItemReaderBuilder<WeeklyAggregationRow>()
            .name(READER_NAME)
            .dataSource(dataSource)
            .fetchSize(CHUNK_SIZE)
            .sql(
                """
                SELECT
                    product_id,
                    SUM(likes_count) AS total_likes,
                    SUM(views_count) AS total_views,
                    SUM(sales_count) AS total_sales,
                    (SUM(likes_count) * ? + SUM(views_count) * ? + SUM(sales_count) * ?) AS score
                FROM product_metrics_daily
                WHERE metric_date BETWEEN ? AND ?
                  AND deleted_at IS NULL
                GROUP BY product_id
                ORDER BY score DESC, product_id ASC
                LIMIT ?
                """.trimIndent(),
            )
            .preparedStatementSetter { ps ->
                ps.setDouble(1, scorePolicy.likeWeight)
                ps.setDouble(2, scorePolicy.viewWeight)
                ps.setDouble(3, scorePolicy.orderWeight)
                ps.setDate(4, Date.valueOf(period.start))
                ps.setDate(5, Date.valueOf(period.end))
                ps.setInt(6, TOP_N)
            }
            .rowMapper { rs, _ ->
                WeeklyAggregationRow(
                    productId = rs.getLong("product_id"),
                    totalLikes = rs.getLong("total_likes"),
                    totalViews = rs.getLong("total_views"),
                    totalSales = rs.getLong("total_sales"),
                    score = rs.getDouble("score"),
                )
            }
            .build()
    }

    @Bean
    fun weeklyRankingItemWriter(): JpaItemWriter<WeeklyProductRankModel> {
        return JpaItemWriterBuilder<WeeklyProductRankModel>()
            .entityManagerFactory(entityManagerFactory)
            .build()
    }
}
