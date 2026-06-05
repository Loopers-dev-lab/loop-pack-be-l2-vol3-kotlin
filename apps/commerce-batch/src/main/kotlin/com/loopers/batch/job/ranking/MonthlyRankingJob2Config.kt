package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.domain.ProductRankMonthly
import com.loopers.batch.job.ranking.domain.RankingAggregation
import com.loopers.batch.job.ranking.infrastructure.ProductRankMonthlyJpaRepository
import com.loopers.batch.job.ranking.infrastructure.RankingMetricJpaRepository
import com.loopers.batch.job.ranking.step.AppAggregationReader
import com.loopers.batch.job.ranking.step.ClearRankingTasklet
import com.loopers.batch.job.ranking.step.MonthlyRankingProcessor
import com.loopers.batch.listener.ChunkListener
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.JpaItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MonthlyRankingJob2Config.JOB_NAME)
@Configuration
class MonthlyRankingJob2Config(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val rankingMetricJpaRepository: RankingMetricJpaRepository,
    private val productRankMonthlyJpaRepository: ProductRankMonthlyJpaRepository,
    private val entityManagerFactory: EntityManagerFactory,
    private val transactionManager: PlatformTransactionManager,
) {
    companion object {
        const val JOB_NAME = "monthlyRankingJob2"
        private const val CLEAR_STEP = "clearMonthlyRankingStep2"
        private const val CHUNK_STEP = "monthlyRankingChunkStep2"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM")
    }

    @Bean(JOB_NAME)
    fun monthlyRankingJob2(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(clearMonthlyRankingStep2(null))
            .next(monthlyRankingChunkStep2(null))
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(CLEAR_STEP)
    fun clearMonthlyRankingStep2(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
    ): Step {
        val date = requestDate?.let { LocalDate.parse(it, DATE_FORMAT) } ?: LocalDate.now()
        val yearMonth = date.format(MONTH_FORMAT)
        return StepBuilder(CLEAR_STEP, jobRepository)
            .tasklet(
                ClearRankingTasklet(
                    deleteAction = { productRankMonthlyJpaRepository.deleteByPeriodDate(it) },
                    periodDate = yearMonth,
                ),
                transactionManager,
            )
            .listener(stepMonitorListener)
            .build()
    }

    @JobScope
    @Bean(CHUNK_STEP)
    fun monthlyRankingChunkStep2(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
    ): Step {
        val date = requestDate?.let { LocalDate.parse(it, DATE_FORMAT) } ?: LocalDate.now()
        val yearMonth = date.format(MONTH_FORMAT)
        val ym = YearMonth.from(date)
        val startDate = ym.atDay(1).format(DATE_FORMAT)
        val endDate = ym.atEndOfMonth().format(DATE_FORMAT)

        return StepBuilder(CHUNK_STEP, jobRepository)
            .chunk<RankingAggregation, ProductRankMonthly>(100, transactionManager)
            .reader(AppAggregationReader(rankingMetricJpaRepository, startDate, endDate))
            .processor(MonthlyRankingProcessor(yearMonth))
            .writer(jpaItemWriter())
            .listener(chunkListener)
            .listener(stepMonitorListener)
            .build()
    }

    private fun jpaItemWriter(): JpaItemWriter<ProductRankMonthly> {
        return JpaItemWriter<ProductRankMonthly>().apply {
            setEntityManagerFactory(entityManagerFactory)
        }
    }
}
