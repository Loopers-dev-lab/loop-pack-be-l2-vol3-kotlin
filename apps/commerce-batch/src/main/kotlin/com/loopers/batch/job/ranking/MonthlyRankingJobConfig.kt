package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.step.CleanupRankingTasklet
import com.loopers.batch.job.ranking.step.RankingWriter
import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import com.loopers.infrastructure.ranking.ProductRankMonthlyEntity
import com.loopers.infrastructure.ranking.ProductRankMonthlyJpaRepository
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.sql.DataSource

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MonthlyRankingJobConfig.JOB_NAME)
@Configuration
class MonthlyRankingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val monthlyJpaRepository: ProductRankMonthlyJpaRepository,
) {
    companion object {
        const val JOB_NAME = "monthlyRankingJob"
        private const val STEP_CLEANUP = "cleanupMonthlyRankingStep"
        private const val STEP_AGGREGATE = "aggregateMonthlyRankingStep"
        private const val CHUNK_SIZE = 100
        private const val RANKING_LIMIT = 100
    }

    @Bean(JOB_NAME)
    fun monthlyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(cleanupMonthlyRankingStep(null))
            .next(aggregateMonthlyRankingStep(null, null))
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_CLEANUP)
    fun cleanupMonthlyRankingStep(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
    ): Step {
        val date = LocalDate.parse(requireNotNull(requestDate), DATE_FORMATTER)
        val startOfMonth = date.with(TemporalAdjusters.firstDayOfMonth())
        return StepBuilder(STEP_CLEANUP, jobRepository)
            .tasklet(
                CleanupRankingTasklet(startOfMonth, monthlyJpaRepository::deleteByRankingDate, "mv_product_rank_monthly"),
                transactionManager,
            )
            .listener(stepMonitorListener)
            .build()
    }

    @JobScope
    @Bean(STEP_AGGREGATE)
    fun aggregateMonthlyRankingStep(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
        dataSource: DataSource?,
    ): Step {
        val date = LocalDate.parse(requireNotNull(requestDate), DATE_FORMATTER)
        val startOfMonth = date.with(TemporalAdjusters.firstDayOfMonth())
        val writer = monthlyRankingWriter(startOfMonth)
        return StepBuilder(STEP_AGGREGATE, jobRepository)
            .chunk<ProductMetricsRow, ProductMetricsRow>(CHUNK_SIZE, transactionManager)
            .reader(monthlyProductMetricsReader(requireNotNull(dataSource)))
            .writer(writer)
            .listener(writer as org.springframework.batch.core.StepExecutionListener)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()
    }

    @StepScope
    @Bean("monthlyProductMetricsReader")
    fun monthlyProductMetricsReader(dataSource: DataSource): JdbcCursorItemReader<ProductMetricsRow> {
        return JdbcCursorItemReaderBuilder<ProductMetricsRow>()
            .name("monthlyProductMetricsReader")
            .dataSource(dataSource)
            .sql(
                """
                SELECT product_id, view_count, like_count, order_count, sales_amount,
                       (view_count * 0.1 + like_count * 0.2 + order_count * 0.7) AS score
                FROM product_metrics
                ORDER BY score DESC
                LIMIT $RANKING_LIMIT
                """.trimIndent(),
            )
            .rowMapper { rs, _ ->
                ProductMetricsRow(
                    productId = rs.getLong("product_id"),
                    viewCount = rs.getLong("view_count"),
                    likeCount = rs.getLong("like_count"),
                    orderCount = rs.getLong("order_count"),
                    salesAmount = rs.getLong("sales_amount"),
                    score = rs.getDouble("score"),
                )
            }
            .build()
    }

    private fun monthlyRankingWriter(rankingDate: LocalDate): RankingWriter<ProductRankMonthlyEntity> {
        return RankingWriter(
            rankingDate = rankingDate,
            entityFactory = { row, rank, date ->
                ProductRankMonthlyEntity(
                    id = null,
                    productId = row.productId,
                    score = row.score,
                    ranking = rank,
                    rankingDate = date,
                )
            },
            saveAction = { entities -> monthlyJpaRepository.saveAll(entities) },
        )
    }
}

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
