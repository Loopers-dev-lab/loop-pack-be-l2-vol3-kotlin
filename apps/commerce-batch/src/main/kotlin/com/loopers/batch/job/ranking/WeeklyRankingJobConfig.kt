package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.infrastructure.ProductRankWeeklyJpaRepository
import com.loopers.batch.job.ranking.infrastructure.RankingMetricJpaRepository
import com.loopers.batch.job.ranking.step.WeeklyRankingTasklet
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankingJobConfig.JOB_NAME)
@Configuration
class WeeklyRankingJobConfig(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val rankingMetricJpaRepository: RankingMetricJpaRepository,
    private val productRankWeeklyJpaRepository: ProductRankWeeklyJpaRepository,
    private val transactionManager: PlatformTransactionManager,
) {
    companion object {
        const val JOB_NAME = "weeklyRankingJob"
        private const val STEP_NAME = "weeklyRankingStep"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @Bean(JOB_NAME)
    fun weeklyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(weeklyRankingStep(null))
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_NAME)
    fun weeklyRankingStep(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
    ): Step {
        val date = requestDate?.let { LocalDate.parse(it, DATE_FORMAT) } ?: LocalDate.now()
        val monday = date.with(DayOfWeek.MONDAY)
        val sunday = monday.plusDays(6)

        return StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                WeeklyRankingTasklet(
                    rankingMetricJpaRepository = rankingMetricJpaRepository,
                    productRankWeeklyJpaRepository = productRankWeeklyJpaRepository,
                    startDate = monday.format(DATE_FORMAT),
                    endDate = sunday.format(DATE_FORMAT),
                    periodDate = monday.format(DATE_FORMAT),
                ),
                transactionManager,
            )
            .listener(stepMonitorListener)
            .build()
    }
}
