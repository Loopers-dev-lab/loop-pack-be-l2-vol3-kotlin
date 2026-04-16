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
 * 주간 랭킹 집계 Batch Job.
 *
 * 3-Step 구조:
 * - Step 1 (Chunk): product_metrics 주간 합산 → score 계산 → 중간 테이블 저장
 * - Step 2 (Tasklet): 중간 테이블에서 TOP 100 → MV에 새 version INSERT → 중간 테이블 cleanup
 * - Step 3 (Tasklet): 이전 version 데이터 삭제
 *
 * 실행: --job.name=weeklyRankingJob --periodType=WEEKLY --periodKey=2026-W15 --startDate=2026-04-06 --endDate=2026-04-12
 */
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankingJobConfig.JOB_NAME)
@Configuration
class WeeklyRankingJobConfig(
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
        const val JOB_NAME = "weeklyRankingJob"
        private const val STEP_AGGREGATE = "weeklyAggregateStep"
        private const val STEP_RANK_AND_STORE = "weeklyRankAndStoreStep"
        private const val STEP_CLEANUP = "weeklyCleanupStep"
    }

    /**
     * Job 정의: 3개 Step을 순차 실행한다.
     * Step 1 실패 시 → Step 2, 3 실행 안 됨 (중간 테이블에 미완성 데이터만 남음, 재실행으로 복구)
     * Step 2 실패 시 → MV에 새 version이 안 들어감 (API는 이전 version으로 정상 서빙)
     * Step 3 실패 시 → 이전 version이 남아있을 뿐, API에 영향 없음
     */
    @Bean(JOB_NAME)
    fun weeklyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(weeklyAggregateStep())
            .next(weeklyRankAndStoreStep())
            .next(weeklyCleanupStep())
            .listener(jobListener)
            .build()
    }

    /**
     * Step 1 — Chunk-Oriented 집계.
     *
     * product_metrics에서 startDate~endDate 범위를 GROUP BY product_id로 합산하고,
     * 가중합산 score를 계산하여 중간 테이블(ranking_aggregate_temp)에 적재한다.
     * 1000건씩 chunk 단위 트랜잭션으로 처리 — 중간 실패 시 마지막 커밋 chunk부터 재시작.
     *
     * Reader: JdbcPagingItemReader (SQL GROUP BY로 DB에서 합산)
     * Processor: score = view*가중치 + like*가중치 + order*가중치
     * Writer: 중간 테이블에 batch INSERT
     */
    @Bean(STEP_AGGREGATE)
    fun weeklyAggregateStep(): Step {
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
     * Step 2 — TOP 100 선정 + MV 적재.
     *
     * 중간 테이블에서 score DESC 정렬하여 TOP 100을 뽑고,
     * MV(mv_product_rank_weekly)에 새 version으로 INSERT한다.
     * 블루-그린 교체: 기존 데이터를 삭제하지 않고 새 version을 추가하므로,
     * 적재 중에도 API는 이전 version으로 정상 서빙된다.
     * INSERT 완료 후 중간 테이블을 cleanup한다.
     */
    @Bean(STEP_RANK_AND_STORE)
    fun weeklyRankAndStoreStep(): Step {
        return StepBuilder(STEP_RANK_AND_STORE, jobRepository)
            .tasklet(rankAndStoreMvTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }

    /**
     * Step 3 — 이전 version 삭제.
     *
     * Step 2에서 새 version이 완전히 적재된 후 실행된다.
     * MV에서 최신 version보다 낮은 version의 데이터를 삭제한다.
     * 이 Step이 실패해도 API에는 영향 없다 (이전 데이터가 남아있을 뿐).
     */
    @Bean(STEP_CLEANUP)
    fun weeklyCleanupStep(): Step {
        return StepBuilder(STEP_CLEANUP, jobRepository)
            .tasklet(cleanupPreviousVersionTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }
}
