package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.domain.ProductRankWeekly
import com.loopers.batch.job.ranking.domain.RankingAggregation
import com.loopers.batch.job.ranking.infrastructure.ProductRankWeeklyJpaRepository
import com.loopers.batch.job.ranking.infrastructure.RankingMetricJpaRepository
import com.loopers.batch.job.ranking.step.AppAggregationReader
import com.loopers.batch.job.ranking.step.ClearRankingTasklet
import com.loopers.batch.job.ranking.step.WeeklyRankingProcessor
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankingJob2Config.JOB_NAME)
@Configuration
class WeeklyRankingJob2Config(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val rankingMetricJpaRepository: RankingMetricJpaRepository,
    private val productRankWeeklyJpaRepository: ProductRankWeeklyJpaRepository,
    private val entityManagerFactory: EntityManagerFactory,
    private val transactionManager: PlatformTransactionManager,
) {
    companion object {
        const val JOB_NAME = "weeklyRankingJob2"
        private const val CLEAR_STEP = "clearWeeklyRankingStep2"
        private const val CHUNK_STEP = "weeklyRankingChunkStep2"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @Bean(JOB_NAME)
    fun weeklyRankingJob2(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(clearWeeklyRankingStep2(null))
            .next(weeklyRankingChunkStep2(null))
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(CLEAR_STEP)
    fun clearWeeklyRankingStep2(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
    ): Step {
        val monday = resolveMonday(requestDate)
        return StepBuilder(CLEAR_STEP, jobRepository)
            .tasklet(
                ClearRankingTasklet(
                    deleteAction = { productRankWeeklyJpaRepository.deleteByPeriodDate(it) },
                    periodDate = monday,
                ),
                transactionManager,
            )
            .listener(stepMonitorListener)
            .build()
    }

    @JobScope
    @Bean(CHUNK_STEP)
    fun weeklyRankingChunkStep2(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
    ): Step {
        val monday = resolveMonday(requestDate)
        val sunday = LocalDate.parse(monday, DATE_FORMAT).plusDays(6).format(DATE_FORMAT)

        return StepBuilder(CHUNK_STEP, jobRepository)
            .chunk<RankingAggregation, ProductRankWeekly>(100, transactionManager)
            .reader(AppAggregationReader(rankingMetricJpaRepository, monday, sunday))
            .processor(WeeklyRankingProcessor(monday))
            .writer(jpaItemWriter())
            .listener(chunkListener)
            .listener(stepMonitorListener)
            .build()
    }

    private fun jpaItemWriter(): JpaItemWriter<ProductRankWeekly> {
        return JpaItemWriter<ProductRankWeekly>().apply {
            setEntityManagerFactory(entityManagerFactory)
        }
    }

    private fun resolveMonday(requestDate: String?): String {
        val date = requestDate?.let { LocalDate.parse(it, DATE_FORMAT) } ?: LocalDate.now()
        return date.with(DayOfWeek.MONDAY).format(DATE_FORMAT)
    }
}
