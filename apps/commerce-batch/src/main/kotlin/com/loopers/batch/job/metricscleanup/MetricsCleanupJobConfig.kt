package com.loopers.batch.job.metricscleanup

import com.loopers.batch.job.metricscleanup.step.MetricsCleanupTasklet
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

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MetricsCleanupJobConfig.JOB_NAME)
@Configuration
class MetricsCleanupJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val metricsCleanupTasklet: MetricsCleanupTasklet,
) {
    companion object {
        const val JOB_NAME = "metricsCleanupJob"
        private const val STEP_NAME = "metricsCleanupStep"
    }

    @Bean(JOB_NAME)
    fun metricsCleanupJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(metricsCleanupStep())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_NAME)
    fun metricsCleanupStep(): Step {
        return StepBuilder(STEP_NAME, jobRepository)
            .tasklet(metricsCleanupTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }
}
