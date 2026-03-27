package com.loopers.batch.job.cleanup

import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.batch.support.transaction.ResourcelessTransactionManager

@Configuration
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = OutboxEventCleanupJobConfig.JOB_NAME)
class OutboxEventCleanupJobConfig(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
) {
    companion object {
        const val JOB_NAME = "outboxEventCleanupJob"
    }

    @Bean
    fun outboxEventCleanupJob(outboxEventCleanupStep: Step): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .listener(jobListener)
            .start(outboxEventCleanupStep)
            .build()
    }

    @Bean
    fun outboxEventCleanupStep(tasklet: OutboxEventCleanupTasklet): Step {
        return StepBuilder("outboxEventCleanupStep", jobRepository)
            .tasklet(tasklet, ResourcelessTransactionManager())
            .listener(stepMonitorListener)
            .build()
    }
}
