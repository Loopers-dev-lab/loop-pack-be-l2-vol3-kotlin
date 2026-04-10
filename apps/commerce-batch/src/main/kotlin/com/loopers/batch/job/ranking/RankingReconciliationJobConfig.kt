package com.loopers.batch.job.ranking

import com.loopers.batch.listener.ChunkListener
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
import javax.sql.DataSource

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = RankingReconciliationJobConfig.JOB_NAME)
@Configuration
class RankingReconciliationJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val rankingReconciliationProcessor: RankingReconciliationProcessor,
    private val rankingReconciliationWriter: RankingReconciliationWriter,
    private val dataSource: DataSource,
) {
    companion object {
        const val JOB_NAME = "rankingReconciliationJob"
        private const val STEP_NAME = "rankingReconciliationStep"
        private const val CHUNK_SIZE = 100
    }

    @Bean(JOB_NAME)
    fun rankingReconciliationJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(rankingReconciliationStep())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_NAME)
    fun rankingReconciliationStep(): Step {
        val reader = rankingReconciliationReader(dataSource, CHUNK_SIZE)
        reader.afterPropertiesSet()
        return StepBuilder(STEP_NAME, jobRepository)
            .chunk<RankingScore, RankingScore>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(rankingReconciliationProcessor)
            .writer(rankingReconciliationWriter)
            .listener(stepMonitorListener)
            .listener(chunkListener)
            .build()
    }
}
