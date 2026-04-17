package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.RankingBatchConstants.CHUNK_SIZE
import com.loopers.batch.job.ranking.RankingBatchConstants.DATE_FORMATTER
import com.loopers.batch.job.ranking.RankingBatchConstants.PRODUCT_METRICS_RANKING_SQL
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
            .listener(writer)
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
            .sql(PRODUCT_METRICS_RANKING_SQL)
            .rowMapper { rs, _ ->
                ProductMetricsRow(
                    productId = rs.getLong("product_id"),
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
