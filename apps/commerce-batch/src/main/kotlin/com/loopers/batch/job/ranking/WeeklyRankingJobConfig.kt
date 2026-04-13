package com.loopers.batch.job.ranking

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankingJobConfig.JOB_NAME)
@Configuration
class WeeklyRankingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val weeklyRankingTasklet: WeeklyRankingTasklet,
) {
    companion object {
        const val JOB_NAME = "weeklyRankingJob"
        private const val STEP_NAME = "weeklyRankingStep"
    }

    @Bean(JOB_NAME)
    fun weeklyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(WeeklyRankingJobParameterValidator())
            .start(weeklyRankingStep())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_NAME)
    fun weeklyRankingStep(): Step {
        return StepBuilder(STEP_NAME, jobRepository)
            .tasklet(weeklyRankingTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }
}
