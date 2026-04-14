package com.loopers.batch.job.ranking

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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class MonthlyRankingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val monthlyRankingTasklet: MonthlyRankingTasklet,
) {
    companion object {
        const val JOB_NAME = "monthlyRankingJob"
        private const val STEP_NAME = "monthlyRankingStep"
    }

    @Bean(JOB_NAME)
    fun monthlyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(MonthlyRankingJobParameterValidator())
            .start(monthlyRankingStep())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_NAME)
    fun monthlyRankingStep(): Step {
        return StepBuilder(STEP_NAME, jobRepository)
            .tasklet(monthlyRankingTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }
}
