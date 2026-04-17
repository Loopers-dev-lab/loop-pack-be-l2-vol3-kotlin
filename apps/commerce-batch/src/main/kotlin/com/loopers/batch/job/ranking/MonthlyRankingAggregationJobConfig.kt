package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.step.MonthlyRankingProcessor
import com.loopers.batch.job.ranking.step.MonthlyRankingPurgeTasklet
import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import com.loopers.domain.mv.MonthlyPeriod
import com.loopers.domain.mv.MonthlyProductRankModel
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
 * 월간 랭킹 집계 Job.
 *
 * 실행: `--job.name=monthlyRankingAggregationJob --requestDate=yyyyMMdd`
 *
 * - Step 1 (Tasklet): `requestDate` 가 속한 월의 MV 행을 `year_month_val` 기준으로 삭제 (멱등)
 * - Step 2 (Chunk-Oriented): 해당 월 1일~말일의 `product_metrics_daily` 를 집계·정렬·TOP N 컷팅한 뒤
 *   rank_position 을 부여하며 `mv_product_rank_monthly` 에 INSERT
 *
 * Phase 3 의 주간 Job 과 구조가 거의 동일하며, Phase 6 리팩터링에서 공통화 여부를 재검토한다.
 */
@ConditionalOnProperty(
    name = ["spring.batch.job.name"],
    havingValue = MonthlyRankingAggregationJobConfig.JOB_NAME,
)
@Configuration
class MonthlyRankingAggregationJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val dataSource: DataSource,
    private val entityManagerFactory: EntityManagerFactory,
    private val monthlyRankingPurgeTasklet: MonthlyRankingPurgeTasklet,
    private val monthlyRankingProcessor: MonthlyRankingProcessor,
) {
    companion object {
        const val JOB_NAME = "monthlyRankingAggregationJob"
        private const val PURGE_STEP = "monthlyRankingPurgeStep"
        private const val AGGREGATE_STEP = "monthlyRankingAggregateStep"
        private const val CHUNK_SIZE = 50
        private const val TOP_N = 100
        private const val READER_NAME = "monthlyRankingItemReader"
    }

    @Bean(JOB_NAME)
    fun monthlyRankingAggregationJob(
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
    fun monthlyRankingPurgeStep(): Step {
        return StepBuilder(PURGE_STEP, jobRepository)
            .tasklet(monthlyRankingPurgeTasklet, transactionManager)
            .allowStartIfComplete(true)
            .listener(stepMonitorListener)
            .build()
    }

    @JobScope
    @Bean(AGGREGATE_STEP)
    fun monthlyRankingAggregateStep(
        monthlyRankingItemReader: JdbcCursorItemReader<MonthlyAggregationRow>,
        monthlyRankingItemWriter: JpaItemWriter<MonthlyProductRankModel>,
    ): Step {
        return StepBuilder(AGGREGATE_STEP, jobRepository)
            .chunk<MonthlyAggregationRow, MonthlyProductRankModel>(CHUNK_SIZE, transactionManager)
            .reader(monthlyRankingItemReader)
            .processor(monthlyRankingProcessor)
            .writer(monthlyRankingItemWriter)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()
    }

    @StepScope
    @Bean
    fun monthlyRankingItemReader(
        @Value("#{jobParameters['requestDate']}") requestDate: String,
        scorePolicy: BatchRankingScorePolicy,
    ): JdbcCursorItemReader<MonthlyAggregationRow> {
        val period = MonthlyPeriod.of(LocalDate.parse(requestDate, DateTimeFormatter.BASIC_ISO_DATE))
        return JdbcCursorItemReaderBuilder<MonthlyAggregationRow>()
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
                MonthlyAggregationRow(
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
    fun monthlyRankingItemWriter(): JpaItemWriter<MonthlyProductRankModel> {
        return JpaItemWriterBuilder<MonthlyProductRankModel>()
            .entityManagerFactory(entityManagerFactory)
            .build()
    }
}
