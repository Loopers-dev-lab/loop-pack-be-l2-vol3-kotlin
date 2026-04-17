package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.RankingBatchConstants.CHUNK_SIZE
import com.loopers.batch.job.ranking.RankingBatchConstants.DATE_FORMATTER
import com.loopers.batch.job.ranking.RankingBatchConstants.PRODUCT_METRICS_RANKING_SQL
import com.loopers.batch.job.ranking.step.CleanupRankingTasklet
import com.loopers.batch.job.ranking.step.RankingWriter
import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import com.loopers.infrastructure.ranking.ProductRankWeeklyEntity
import com.loopers.infrastructure.ranking.ProductRankWeeklyJpaRepository
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
import java.time.DayOfWeek
import java.time.LocalDate
import javax.sql.DataSource

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankingJobConfig.JOB_NAME)
@Configuration
class WeeklyRankingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val weeklyJpaRepository: ProductRankWeeklyJpaRepository,
) {
    companion object {
        const val JOB_NAME = "weeklyRankingJob"
        private const val STEP_CLEANUP = "cleanupWeeklyRankingStep"
        private const val STEP_AGGREGATE = "aggregateWeeklyRankingStep"
    }

    @Bean(JOB_NAME)
    fun weeklyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(cleanupWeeklyRankingStep(null))
            .next(aggregateWeeklyRankingStep(null, null))
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_CLEANUP)
    fun cleanupWeeklyRankingStep(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
    ): Step {
        val date = LocalDate.parse(requireNotNull(requestDate), DATE_FORMATTER)
        val startOfWeek = date.with(DayOfWeek.MONDAY)
        return StepBuilder(STEP_CLEANUP, jobRepository)
            .tasklet(
                CleanupRankingTasklet(startOfWeek, weeklyJpaRepository::deleteByRankingDate, "mv_product_rank_weekly"),
                transactionManager,
            )
            .listener(stepMonitorListener)
            .build()
    }

    @JobScope
    @Bean(STEP_AGGREGATE)
    fun aggregateWeeklyRankingStep(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
        dataSource: DataSource?,
    ): Step {
        val date = LocalDate.parse(requireNotNull(requestDate), DATE_FORMATTER)
        val startOfWeek = date.with(DayOfWeek.MONDAY)
        val writer = weeklyRankingWriter(startOfWeek)
        return StepBuilder(STEP_AGGREGATE, jobRepository)
            .chunk<ProductMetricsRow, ProductMetricsRow>(CHUNK_SIZE, transactionManager)
            .reader(productMetricsReader(requireNotNull(dataSource)))
            .writer(writer)
            .listener(writer)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()
    }

    @StepScope
    @Bean("weeklyProductMetricsReader")
    fun productMetricsReader(dataSource: DataSource): JdbcCursorItemReader<ProductMetricsRow> {
        return JdbcCursorItemReaderBuilder<ProductMetricsRow>()
            .name("weeklyProductMetricsReader")
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

    private fun weeklyRankingWriter(rankingDate: LocalDate): RankingWriter<ProductRankWeeklyEntity> {
        return RankingWriter(
            rankingDate = rankingDate,
            entityFactory = { row, rank, date ->
                ProductRankWeeklyEntity(
                    id = null,
                    productId = row.productId,
                    score = row.score,
                    ranking = rank,
                    rankingDate = date,
                )
            },
            saveAction = { entities -> weeklyJpaRepository.saveAll(entities) },
        )
    }
}
