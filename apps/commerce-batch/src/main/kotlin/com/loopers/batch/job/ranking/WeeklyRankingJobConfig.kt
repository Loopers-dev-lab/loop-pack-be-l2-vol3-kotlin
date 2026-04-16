package com.loopers.batch.job.ranking

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
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val txManager: PlatformTransactionManager,
    private val reader: WeeklyRankingMetricsReader,
    private val processor: MetricsAggregationProcessor,
    private val writer: WeeklyRankingChunkWriter,
    private val rankingAndTrimTasklet: WeeklyRankingAndTrimTasklet,
) {
    companion object {
        const val JOB_NAME = "weeklyRankingJob"
        private const val ACCUMULATION_STEP_NAME = "weeklyAccumulationStep"
        private const val RANKING_TRIM_STEP_NAME = "weeklyRankingAndTrimStep"
    }

    @Bean(JOB_NAME)
    fun weeklyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(accumulationStep())
            .next(rankingAndTrimStep())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(ACCUMULATION_STEP_NAME)
    fun accumulationStep(): Step {
        return StepBuilder(ACCUMULATION_STEP_NAME, jobRepository)
            .chunk<ProductMetricsDailyRow, RankingScoreContribution>(500, txManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .listener(stepMonitorListener)
            .build()
    }

    @JobScope
    @Bean(RANKING_TRIM_STEP_NAME)
    fun rankingAndTrimStep(): Step {
        return StepBuilder(RANKING_TRIM_STEP_NAME, jobRepository)
            .tasklet(rankingAndTrimTasklet, txManager)
            .build()
    }
}
