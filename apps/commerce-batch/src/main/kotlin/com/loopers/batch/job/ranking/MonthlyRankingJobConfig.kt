package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.step.CleanupPreviousVersionTasklet
import com.loopers.batch.job.ranking.step.RankAndStoreMvTasklet
import com.loopers.batch.job.ranking.step.RankingAggregateProcessor
import com.loopers.batch.job.ranking.step.RankingAggregateReader
import com.loopers.batch.job.ranking.step.RankingAggregateWriter
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import com.loopers.domain.ranking.MetricsAggregateDto
import com.loopers.domain.ranking.RankingAggregateTemp
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
 * 월간 랭킹 집계 Batch Job.
 *
 * 3-Step 구조 (WeeklyRankingJobConfig와 동일 패턴, 날짜 범위만 월 기준):
 * - Step 1 (Chunk): product_metrics 월간 합산 → score 계산 → 중간 테이블 저장
 * - Step 2 (Tasklet): 중간 테이블에서 TOP 100 → MV에 새 version INSERT → 중간 테이블 cleanup
 * - Step 3 (Tasklet): 이전 version 데이터 삭제
 *
 * 실행: --job.name=monthlyRankingJob --periodType=MONTHLY --periodKey=2026-04 --startDate=2026-04-01 --endDate=2026-04-30
 */
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MonthlyRankingJobConfig.JOB_NAME)
@Configuration
class MonthlyRankingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val rankingAggregateReader: RankingAggregateReader,
    private val rankingAggregateProcessor: RankingAggregateProcessor,
    private val rankingAggregateWriter: RankingAggregateWriter,
    private val rankAndStoreMvTasklet: RankAndStoreMvTasklet,
    private val cleanupPreviousVersionTasklet: CleanupPreviousVersionTasklet,
) {
    companion object {
        const val JOB_NAME = "monthlyRankingJob"
        private const val STEP_AGGREGATE = "monthlyAggregateStep"
        private const val STEP_RANK_AND_STORE = "monthlyRankAndStoreStep"
        private const val STEP_CLEANUP = "monthlyCleanupStep"
    }

    /**
     * Job 정의: WeeklyRankingJob과 동일한 3-Step 구조.
     * 차이점은 JobParameters의 periodType=MONTHLY, 날짜 범위가 월 기준이라는 것뿐.
     * 실패 시 동작도 Weekly와 동일 (Step별 독립적 실패 격리).
     */
    @Bean(JOB_NAME)
    fun monthlyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(monthlyAggregateStep())
            .next(monthlyRankAndStoreStep())
            .next(monthlyCleanupStep())
            .listener(jobListener)
            .build()
    }

    /**
     * Step 1 — Chunk-Oriented 집계.
     * 동일한 Reader/Processor/Writer를 공유한다 (Weekly와 같은 @StepScope 컴포넌트).
     * JobParameters의 startDate/endDate만 다르므로 월 단위 합산이 된다.
     */
    @Bean(STEP_AGGREGATE)
    fun monthlyAggregateStep(): Step {
        return StepBuilder(STEP_AGGREGATE, jobRepository)
            .chunk<MetricsAggregateDto, RankingAggregateTemp>(
                RankingAggregateReader.CHUNK_SIZE,
                transactionManager,
            )
            .reader(rankingAggregateReader)
            .processor(rankingAggregateProcessor)
            .writer(rankingAggregateWriter)
            .listener(stepMonitorListener)
            .build()
    }

    /**
     * Step 2 — TOP 100 선정 + MV 적재 (mv_product_rank_monthly).
     * periodType=MONTHLY 이므로 RankAndStoreMvTasklet이 월간 MV에 INSERT한다.
     */
    @Bean(STEP_RANK_AND_STORE)
    fun monthlyRankAndStoreStep(): Step {
        return StepBuilder(STEP_RANK_AND_STORE, jobRepository)
            .tasklet(rankAndStoreMvTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }

    /**
     * Step 3 — 이전 version 삭제 (mv_product_rank_monthly).
     */
    @Bean(STEP_CLEANUP)
    fun monthlyCleanupStep(): Step {
        return StepBuilder(STEP_CLEANUP, jobRepository)
            .tasklet(cleanupPreviousVersionTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }
}
