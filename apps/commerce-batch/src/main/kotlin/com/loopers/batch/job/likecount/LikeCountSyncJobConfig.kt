package com.loopers.batch.job.likecount

import com.loopers.batch.job.likecount.step.SyncLikeCountTasklet
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

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = LikeCountSyncJobConfig.JOB_NAME)
@Configuration
class LikeCountSyncJobConfig(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val syncLikeCountTasklet: SyncLikeCountTasklet,
) {
    companion object {
        const val JOB_NAME = "likeCountSyncJob"
        private const val STEP_SYNC_LIKE_COUNT = "syncLikeCountStep"
    }

    @Bean(JOB_NAME)
    fun likeCountSyncJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(syncLikeCountStep())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_SYNC_LIKE_COUNT)
    fun syncLikeCountStep(): Step {
        return StepBuilder(STEP_SYNC_LIKE_COUNT, jobRepository)
            .tasklet(syncLikeCountTasklet, ResourcelessTransactionManager())
            .listener(stepMonitorListener)
            .build()
    }
}
