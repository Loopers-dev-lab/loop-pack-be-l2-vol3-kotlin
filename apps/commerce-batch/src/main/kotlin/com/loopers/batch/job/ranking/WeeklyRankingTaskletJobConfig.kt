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
 * 주간 랭킹 집계 — Tasklet 방식 (대안).
 *
 * Chunk 3-Step과 동일한 결과를 단일 Tasklet(SQL 1발)로 생성한다.
 * 벤치마크 비교 대상으로, Chunk 방식과의 속도/안정성 트레이드오프를 실측한다.
 *
 * 실행: --job.name=weeklyRankingTaskletJob --periodType=WEEKLY --periodKey=2026-W15 --startDate=2026-04-06 --endDate=2026-04-12
 */
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankingTaskletJobConfig.JOB_NAME)
@Configuration
class WeeklyRankingTaskletJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val rankingMvTasklet: RankingMvTasklet,
) {
    companion object {
        const val JOB_NAME = "weeklyRankingTaskletJob"
        private const val STEP_NAME = "weeklyRankingTaskletStep"
    }

    @Bean(JOB_NAME)
    fun weeklyRankingTaskletJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(weeklyRankingTaskletStep())
            .listener(jobListener)
            .build()
    }

    /**
     * 단일 Step — SQL 1발로 집계+rank+MV적재+이전version삭제까지 처리.
     * Chunk 3-Step 대비 코드 단순, 속도 우위. 대신 중간 실패 시 전체 롤백.
     */
    @Bean(STEP_NAME)
    fun weeklyRankingTaskletStep(): Step {
        return StepBuilder(STEP_NAME, jobRepository)
            .tasklet(rankingMvTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }
}
