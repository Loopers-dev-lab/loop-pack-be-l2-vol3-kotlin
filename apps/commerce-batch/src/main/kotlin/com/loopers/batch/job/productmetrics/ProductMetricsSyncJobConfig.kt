package com.loopers.batch.job.productmetrics

import com.loopers.batch.job.productmetrics.step.ProductMetricsSyncTasklet
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.support.transaction.ResourcelessTransactionManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = ProductMetricsSyncJobConfig.JOB_NAME)
@Configuration
class ProductMetricsSyncJobConfig(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val productMetricsSyncTasklet: ProductMetricsSyncTasklet,
) {
    companion object {
        const val JOB_NAME = "productMetricsSyncJob"
        private const val STEP_NAME = "productMetricsSyncStep"
    }

    @Bean(JOB_NAME)
    fun productMetricsSyncJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(productMetricsSyncStep())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_NAME)
    fun productMetricsSyncStep(): Step {
        return StepBuilder(STEP_NAME, jobRepository)
            .tasklet(productMetricsSyncTasklet, ResourcelessTransactionManager())
            .listener(stepMonitorListener)
            .build()
    }
}
