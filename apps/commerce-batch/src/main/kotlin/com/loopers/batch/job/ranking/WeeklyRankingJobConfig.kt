package com.loopers.batch.job.ranking

import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import com.loopers.infrastructure.ranking.ProductRankWeeklyEntity
import com.loopers.infrastructure.ranking.ProductRankWeeklyJpaRepository
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.beans.factory.annotation.Qualifier
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
    jobRepository: JobRepository,
    transactionManager: PlatformTransactionManager,
    jobListener: JobListener,
    stepMonitorListener: StepMonitorListener,
    chunkListener: ChunkListener,
    private val weeklyJpaRepository: ProductRankWeeklyJpaRepository,
) : AbstractRankingJobConfig<ProductRankWeeklyEntity>(
    jobRepository,
    transactionManager,
    jobListener,
    stepMonitorListener,
    chunkListener,
) {
    companion object {
        const val JOB_NAME = "weeklyRankingJob"
        const val STEP_CLEANUP = "cleanupWeeklyRankingStep"
        const val STEP_AGGREGATE = "aggregateWeeklyRankingStep"
        const val READER_NAME = "weeklyProductMetricsReader"
        const val MV_TABLE = "mv_product_rank_weekly"
    }

    override val jobName: String = JOB_NAME
    override val stepCleanupName: String = STEP_CLEANUP
    override val stepAggregateName: String = STEP_AGGREGATE
    override val readerName: String = READER_NAME
    override val mvTableName: String = MV_TABLE

    override fun startOfPeriod(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)
    override fun endOfPeriod(start: LocalDate): LocalDate = start.plusWeeks(1)

    override fun deleteByRankingDate(date: LocalDate) {
        weeklyJpaRepository.deleteByRankingDate(date)
    }

    override fun saveAll(entities: List<ProductRankWeeklyEntity>) {
        weeklyJpaRepository.saveAll(entities)
    }

    override fun newEntity(row: ProductMetricsRow, rank: Int, date: LocalDate): ProductRankWeeklyEntity {
        return ProductRankWeeklyEntity(
            id = null,
            productId = row.productId,
            score = row.score,
            ranking = rank,
            rankingDate = date,
        )
    }

    @Bean(JOB_NAME)
    fun weeklyRankingJob(
        @Qualifier(STEP_CLEANUP) cleanupStep: Step,
        @Qualifier(STEP_AGGREGATE) aggregateStep: Step,
    ): Job = buildJob(cleanupStep, aggregateStep)

    @JobScope
    @Bean(STEP_CLEANUP)
    fun cleanupWeeklyRankingStep(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
    ): Step = buildCleanupStep(requestDate)

    @JobScope
    @Bean(STEP_AGGREGATE)
    fun aggregateWeeklyRankingStep(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
        @Qualifier(READER_NAME) reader: JdbcCursorItemReader<ProductMetricsRow>,
    ): Step = buildAggregateStep(requestDate, reader)

    @StepScope
    @Bean(READER_NAME)
    fun weeklyProductMetricsReader(
        dataSource: DataSource,
        @Value("#{jobParameters['requestDate']}") requestDate: String,
    ): JdbcCursorItemReader<ProductMetricsRow> = buildReader(dataSource, requestDate)
}
