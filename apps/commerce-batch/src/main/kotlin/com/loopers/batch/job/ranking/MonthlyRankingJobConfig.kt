package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.infrastructure.ProductRankMonthlyJpaRepository
import com.loopers.batch.job.ranking.infrastructure.RankingMetricJpaRepository
import com.loopers.batch.job.ranking.step.MonthlyRankingTasklet
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MonthlyRankingJobConfig.JOB_NAME)
@Configuration
class MonthlyRankingJobConfig(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val rankingMetricJpaRepository: RankingMetricJpaRepository,
    private val productRankMonthlyJpaRepository: ProductRankMonthlyJpaRepository,
    private val transactionManager: PlatformTransactionManager,
) {
    companion object {
        const val JOB_NAME = "monthlyRankingJob"
        private const val STEP_NAME = "monthlyRankingStep"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM")
    }

    @Bean(JOB_NAME)
    fun monthlyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(monthlyRankingStep(null))
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_NAME)
    fun monthlyRankingStep(
        @Value("#{jobParameters['requestDate']}") requestDate: String?,
    ): Step {
        val date = requestDate?.let { LocalDate.parse(it, DATE_FORMAT) } ?: LocalDate.now()
        val ym = YearMonth.from(date)

        return StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                MonthlyRankingTasklet(
                    rankingMetricJpaRepository = rankingMetricJpaRepository,
                    productRankMonthlyJpaRepository = productRankMonthlyJpaRepository,
                    startDate = ym.atDay(1).format(DATE_FORMAT),
                    endDate = ym.atEndOfMonth().format(DATE_FORMAT),
                    periodDate = date.format(MONTH_FORMAT),
                ),
                transactionManager,
            )
            .listener(stepMonitorListener)
            .build()
    }
}
