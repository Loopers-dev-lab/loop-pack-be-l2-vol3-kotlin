package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.step.RankingMvTasklet
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
import org.springframework.transaction.PlatformTransactionManager

/**
 * 월간 랭킹 집계 — Tasklet 방식 (대안).
 *
 * 실행: --job.name=monthlyRankingTaskletJob --periodType=MONTHLY --periodKey=2026-04 --startDate=2026-04-01 --endDate=2026-04-30
 */
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MonthlyRankingTaskletJobConfig.JOB_NAME)
@Configuration
class MonthlyRankingTaskletJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val rankingMvTasklet: RankingMvTasklet,
) {
    companion object {
        const val JOB_NAME = "monthlyRankingTaskletJob"
        private const val STEP_NAME = "monthlyRankingTaskletStep"
    }

    @Bean(JOB_NAME)
    fun monthlyRankingTaskletJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(monthlyRankingTaskletStep())
            .listener(jobListener)
            .build()
    }

    @Bean(STEP_NAME)
    fun monthlyRankingTaskletStep(): Step {
        return StepBuilder(STEP_NAME, jobRepository)
            .tasklet(rankingMvTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }
}
