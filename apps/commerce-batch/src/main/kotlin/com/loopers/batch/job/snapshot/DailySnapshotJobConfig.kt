package com.loopers.batch.job.snapshot

import com.loopers.batch.listener.StepMonitorListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.TransientDataAccessException
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class DailySnapshotJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: DailySnapshotJobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val itemReader: DailySnapshotItemReader,
    private val itemWriter: DailySnapshotItemWriter,
    private val properties: DailySnapshotProperties,
) {

    @Bean(JOB_NAME)
    fun dailySnapshotJob(): Job = JobBuilder(JOB_NAME, jobRepository)
        .start(dailySnapshotStep())
        .listener(jobListener)
        .build()

    @JobScope
    @Bean(STEP_NAME)
    fun dailySnapshotStep(): Step = StepBuilder(STEP_NAME, jobRepository)
        .chunk<RankedSnapshot, RankedSnapshot>(properties.chunkSize, transactionManager)
        .reader(itemReader)
        .writer(itemWriter)
        .faultTolerant()
        .skip(NumberFormatException::class.java)
        .skipLimit(properties.skipLimit)
        .retry(RedisConnectionFailureException::class.java)
        .retry(TransientDataAccessException::class.java)
        .retryLimit(properties.retryLimit)
        .listener(stepMonitorListener)
        .build()

    companion object {
        const val JOB_NAME = "dailySnapshotJob"
        const val STEP_NAME = "dailySnapshotStep"
    }
}
