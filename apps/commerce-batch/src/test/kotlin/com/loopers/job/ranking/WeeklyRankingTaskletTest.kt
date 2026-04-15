package com.loopers.job.ranking

import com.loopers.batch.job.ranking.WeeklyRankingJobConfig
import com.loopers.batch.job.ranking.WeeklyRankingQueryDao
import com.loopers.batch.ranking.entity.MvProductRankWeeklyBatchEntity
import com.loopers.batch.ranking.entity.ProductMetricsDailyBatchEntity
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest
@SpringBatchTest
@Import(MySqlTestContainersConfig::class)
@TestPropertySource(
    properties = [
        "spring.batch.job.name=${WeeklyRankingJobConfig.JOB_NAME}",
        "spring.batch.job.enabled=false",
    ],
)
class WeeklyRankingTaskletTest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(WeeklyRankingJobConfig.JOB_NAME) private val job: Job,
    private val databaseCleanUp: DatabaseCleanUp,
    @PersistenceContext private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
) {
    @SpyBean
    private lateinit var spyQueryDao: WeeklyRankingQueryDao
    companion object {
        private val runIdSequence = AtomicLong(0)
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        Mockito.reset(spyQueryDao)
    }

    private fun persistMetrics(
        productId: Long,
        metricDate: LocalDate,
        viewCount: Long,
        likeCount: Long,
        salesCount: Long,
    ) {
        transactionTemplate.execute {
            entityManager.persist(
                ProductMetricsDailyBatchEntity(
                    metricDate = metricDate,
                    productId = productId,
                    viewCount = viewCount,
                    likeCount = likeCount,
                    salesCount = salesCount,
                ),
            )
        }
    }

    private fun findAllMvWeekly(): List<MvProductRankWeeklyBatchEntity> {
        return transactionTemplate.execute {
            entityManager
                .createQuery(
                    "SELECT m FROM MvProductRankWeeklyBatchEntity m ORDER BY m.rankNo ASC",
                    MvProductRankWeeklyBatchEntity::class.java,
                )
                .resultList
        } ?: emptyList()
    }

    @DisplayName("소스 데이터가 150건이면 MV에 정확히 100건만 insert된다")
    @Test
    fun insertsExactly100WhenSourceExceeds100() {
        // arrange
        jobLauncherTestUtils.job = job
        val metricDate = LocalDate.of(2024, 1, 15)
        repeat(150) { i ->
            persistMetrics(
                productId = (i + 1).toLong(),
                metricDate = metricDate,
                viewCount = (150 - i).toLong(),
                likeCount = 0L,
                salesCount = 0L,
            )
        }

        // act
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder()
                .addString("baseDate", "20240115")
                .addLong("run.id", runIdSequence.incrementAndGet())
                .toJobParameters(),
        )

        // assert
        assertAll(
            { assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED) },
            { assertThat(findAllMvWeekly()).hasSize(100) },
        )
    }

    @DisplayName("소스 데이터가 50건이면 MV에 50건만 insert된다")
    @Test
    fun insertsAllWhenSourceBelow100() {
        // arrange
        jobLauncherTestUtils.job = job
        val metricDate = LocalDate.of(2024, 1, 15)
        repeat(50) { i ->
            persistMetrics(
                productId = (i + 1).toLong(),
                metricDate = metricDate,
                viewCount = (50 - i).toLong(),
                likeCount = 0L,
                salesCount = 0L,
            )
        }

        // act
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder()
                .addString("baseDate", "20240115")
                .addLong("run.id", runIdSequence.incrementAndGet())
                .toJobParameters(),
        )

        // assert
        assertAll(
            { assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED) },
            { assertThat(findAllMvWeekly()).hasSize(50) },
        )
    }

    @DisplayName("score = 0.1*view + 0.2*like + 0.7*sales 로 계산되고, score == 0인 상품은 제외된다")
    @Test
    fun calculatesScoreCorrectlyAndExcludesZeroScore() {
        // arrange
        jobLauncherTestUtils.job = job
        val metricDate = LocalDate.of(2024, 1, 15)
        // product 1: score = 0.1*10 + 0.2*5 + 0.7*2 = 1.0 + 1.0 + 1.4 = 3.4
        persistMetrics(1L, metricDate, viewCount = 10L, likeCount = 5L, salesCount = 2L)
        // product 2: score = 0 (모두 0) → 제외 대상
        persistMetrics(2L, metricDate, viewCount = 0L, likeCount = 0L, salesCount = 0L)

        // act
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder()
                .addString("baseDate", "20240115")
                .addLong("run.id", runIdSequence.incrementAndGet())
                .toJobParameters(),
        )

        // assert
        val results = findAllMvWeekly()
        assertAll(
            { assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED) },
            { assertThat(results).hasSize(1) },
            { assertThat(results[0].productId).isEqualTo(1L) },
            { assertThat(results[0].score).isCloseTo(3.4, within(1e-9)) },
        )
    }

    @DisplayName("score가 같을 때 productId 오름차순으로 rank_no가 부여된다")
    @Test
    fun assignsRankByProductIdAscWhenScoreIsTied() {
        // arrange
        jobLauncherTestUtils.job = job
        val metricDate = LocalDate.of(2024, 1, 15)
        // 세 상품 모두 동일한 score: 0.1*10 = 1.0
        persistMetrics(3L, metricDate, viewCount = 10L, likeCount = 0L, salesCount = 0L)
        persistMetrics(1L, metricDate, viewCount = 10L, likeCount = 0L, salesCount = 0L)
        persistMetrics(2L, metricDate, viewCount = 10L, likeCount = 0L, salesCount = 0L)

        // act
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder()
                .addString("baseDate", "20240115")
                .addLong("run.id", runIdSequence.incrementAndGet())
                .toJobParameters(),
        )

        // assert
        val results = findAllMvWeekly()
        assertAll(
            { assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED) },
            { assertThat(results).hasSize(3) },
            { assertThat(results[0].rankNo).isEqualTo(1) },
            { assertThat(results[0].productId).isEqualTo(1L) },
            { assertThat(results[1].rankNo).isEqualTo(2) },
            { assertThat(results[1].productId).isEqualTo(2L) },
            { assertThat(results[2].rankNo).isEqualTo(3) },
            { assertThat(results[2].productId).isEqualTo(3L) },
        )
    }

    @DisplayName("DELETE 후 bulkInsert 예외 발생 시 트랜잭션 롤백으로 기존 Top 100이 유지된다 (Tasklet 원자성)")
    @Test
    fun transactionRollbackPreservesExistingTop100() {
        // arrange: 기존 MV에 1건 세팅 (periodKey "2024-W03")
        jobLauncherTestUtils.job = job
        val metricDate = LocalDate.of(2024, 1, 15)
        transactionTemplate.execute {
            entityManager.persist(
                MvProductRankWeeklyBatchEntity(
                    rankNo = 1,
                    productId = 999L,
                    score = 9.9,
                    viewCount = 100L,
                    likeCount = 10L,
                    salesCount = 5L,
                    periodKey = "2024-W03",
                    periodStartDate = metricDate,
                    periodEndDate = metricDate.plusDays(6),
                ),
            )
        }
        assertThat(findAllMvWeekly()).hasSize(1)
        persistMetrics(1L, metricDate, viewCount = 10L, likeCount = 0L, salesCount = 0L)

        // bulkInsert 호출 시 강제 실패 주입 (deleteByPeriodKey는 실제 실행)
        doThrow(RuntimeException("forced failure"))
            .whenever(spyQueryDao)
            .bulkInsert(any(), any(), any(), any())

        // act: 실제 Step 경로로 실행 — deleteByPeriodKey → bulkInsert 예외 → 트랜잭션 롤백
        val jobExecution = jobLauncherTestUtils.launchStep(
            "weeklyRankingStep",
            JobParametersBuilder()
                .addString("baseDate", "20240115")
                .addLong("run.id", runIdSequence.incrementAndGet())
                .toJobParameters(),
        )

        // assert: Step FAILED + DELETE가 실제 호출됐으나 롤백되어 기존 1건 유지
        val stepExecution = jobExecution.stepExecutions.first()
        val remaining = findAllMvWeekly()
        assertAll(
            { assertThat(stepExecution.status).isEqualTo(BatchStatus.FAILED) },
            { verify(spyQueryDao).deleteByPeriodKey(eq("2024-W03")) },
            { assertThat(remaining).hasSize(1) },
            { assertThat(remaining[0].productId).isEqualTo(999L) },
        )
    }

    @DisplayName("동일 periodKey로 재실행 시 이전 Top 100이 완전히 교체된다")
    @Test
    fun replacesEntireRankingOnRerun() {
        // arrange
        jobLauncherTestUtils.job = job
        val metricDate = LocalDate.of(2024, 1, 15)

        // 1차 실행: product 1, 2
        persistMetrics(1L, metricDate, viewCount = 10L, likeCount = 0L, salesCount = 0L)
        persistMetrics(2L, metricDate, viewCount = 5L, likeCount = 0L, salesCount = 0L)
        jobLauncherTestUtils.launchJob(
            JobParametersBuilder()
                .addString("baseDate", "20240115")
                .addLong("run.id", runIdSequence.incrementAndGet())
                .toJobParameters(),
        )
        assertThat(findAllMvWeekly().map { it.productId }).containsExactly(1L, 2L)

        // MV는 유지한 채 소스 데이터(product_metrics_daily)만 교체 (product 1 제거, product 3 추가)
        transactionTemplate.execute {
            entityManager.createQuery("DELETE FROM ProductMetricsDailyBatchEntity").executeUpdate()
        }
        persistMetrics(2L, metricDate, viewCount = 20L, likeCount = 0L, salesCount = 0L)
        persistMetrics(3L, metricDate, viewCount = 5L, likeCount = 0L, salesCount = 0L)

        // act: 동일 periodKey로 2차 실행 — deleteByPeriodKey가 기존 MV를 실제로 지우는지 검증
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder()
                .addString("baseDate", "20240115")
                .addLong("run.id", runIdSequence.incrementAndGet())
                .toJobParameters(),
        )

        // assert: product 1은 사라지고 product 2, 3만 존재 (이전 MV에 있던 product 1이 교체됨을 확인)
        val results = findAllMvWeekly()
        assertAll(
            { assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED) },
            { assertThat(results).hasSize(2) },
            { assertThat(results.map { it.productId }).containsExactlyInAnyOrder(2L, 3L) },
        )
    }
}
