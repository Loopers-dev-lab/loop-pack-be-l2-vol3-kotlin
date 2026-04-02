package com.loopers.batch.job.queue

import com.loopers.batch.job.queue.step.QueueTokenTasklet
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

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = QueueTokenJobConfig.JOB_NAME)
@Configuration
class QueueTokenJobConfig(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val queueTokenTasklet: QueueTokenTasklet,
) {
    companion object {
        const val JOB_NAME = "queueTokenJob"
        private const val STEP_NAME = "queueTokenIssueStep"
    }

    @Bean(JOB_NAME)
    fun queueTokenJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(queueTokenIssueStep())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_NAME)
    fun queueTokenIssueStep(): Step {
        return StepBuilder(STEP_NAME, jobRepository)
            .tasklet(queueTokenTasklet, ResourcelessTransactionManager())
            .listener(stepMonitorListener)
            .build()
    }
}
