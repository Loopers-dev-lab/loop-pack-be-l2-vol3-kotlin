package com.loopers.batch.job.ranking

import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import com.loopers.infrastructure.ranking.ProductRankMonthlyEntity
import com.loopers.infrastructure.ranking.ProductRankMonthlyJpaRepository
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
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.sql.DataSource

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MonthlyRankingJobConfig.JOB_NAME)
@Configuration
class MonthlyRankingJobConfig(
    jobRepository: JobRepository,
    transactionManager: PlatformTransactionManager,
    jobListener: JobListener,
    stepMonitorListener: StepMonitorListener,
    chunkListener: ChunkListener,
    private val monthlyJpaRepository: ProductRankMonthlyJpaRepository,
) : AbstractRankingJobConfig<ProductRankMonthlyEntity>(
    jobRepository,
    transactionManager,
    jobListener,
    stepMonitorListener,
    chunkListener,
) {
    companion object {
        const val JOB_NAME = "monthlyRankingJob"
        const val STEP_CLEANUP = "cleanupMonthlyRankingStep"
        const val STEP_AGGREGATE = "aggregateMonthlyRankingStep"
        const val READER_NAME = "monthlyProductMetricsReader"
        const val MV_TABLE = "mv_product_rank_monthly"
    }

    override val jobName: String = JOB_NAME
    override val stepCleanupName: String = STEP_CLEANUP
    override val stepAggregateName: String = STEP_AGGREGATE
    override val readerName: String = READER_NAME
    override val mvTableName: String = MV_TABLE

    override fun startOfPeriod(date: LocalDate): LocalDate = date.with(TemporalAdjusters.firstDayOfMonth())
    override fun endOfPeriod(start: LocalDate): LocalDate = start.plusMonths(1)

    override fun deleteByRankingDate(date: LocalDate) {
        monthlyJpaRepository.deleteByRankingDate(date)
    }

    override fun saveAll(entities: List<ProductRankMonthlyEntity>) {
        monthlyJpaRepository.saveAll(entities)
    }

    override fun newEntity(row: ProductMetricsRow, rank: Int, date: LocalDate): ProductRankMonthlyEntity {
        return ProductRankMonthlyEntity(
            id = null,
            productId = row.productId,
            score = row.score,
            ranking = rank,
            rankingDate = date,
        )
    }

    @Bean(JOB_NAME)
    fun monthlyRankingJob(
        @Qualifier(STEP_CLEANUP) cleanupStep: Step,
        @Qualifier(STEP_AGGREGATE) aggregateStep: Step,
    ): Job = buildJob(cleanupStep, aggregateStep)

    @JobScope
    @Bean(STEP_CLEANUP)
    fun cleanupMonthlyRankingStep(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
    ): Step = buildCleanupStep(requestDate)

    @JobScope
    @Bean(STEP_AGGREGATE)
    fun aggregateMonthlyRankingStep(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
        @Qualifier(READER_NAME) reader: JdbcCursorItemReader<ProductMetricsRow>,
    ): Step = buildAggregateStep(requestDate, reader)

    @StepScope
    @Bean(READER_NAME)
    fun monthlyProductMetricsReader(
        dataSource: DataSource,
        @Value("#{jobParameters['requestDate']}") requestDate: String,
    ): JdbcCursorItemReader<ProductMetricsRow> = buildReader(dataSource, requestDate)
}
