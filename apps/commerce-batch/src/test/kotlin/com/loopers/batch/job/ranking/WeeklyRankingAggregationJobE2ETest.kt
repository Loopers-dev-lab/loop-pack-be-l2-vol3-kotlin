package com.loopers.batch.job.ranking

import com.loopers.domain.metrics.ProductMetricsDailyModel
import com.loopers.infrastructure.metrics.ProductMetricsDailyJpaRepository
import com.loopers.infrastructure.mv.WeeklyProductRankJpaRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.condition.EnabledIf
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

/**
 * 주간 랭킹 집계 Job 의 End-to-End 테스트.
 *
 * - Testcontainers MySQL 8 + 실제 Spring Batch 컨텍스트로 동작.
 * - `product_metrics_daily` 를 시드하여 배치를 실행하고 `mv_product_rank_weekly` 적재 결과를 검증한다.
 * - 체크리스트 검증:
 *   - 파라미터 기반 동작 (`requestDate` 미지정 시 FAILED)
 *   - Chunk-Oriented 집계 (정상 적재, 경계 외 무시, 동점 안정 정렬)
 *   - 재실행 멱등성 (purge Tasklet 의 효과)
 */
@SpringBootTest
@SpringBatchTest
@Import(MySqlTestContainersConfig::class)
@TestPropertySource(properties = ["spring.batch.job.name=${WeeklyRankingAggregationJobConfig.JOB_NAME}"])
@EnabledIf(
    value = "com.loopers.batch.job.ranking.WeeklyRankingAggregationJobE2ETest#isDockerAvailable",
    disabledReason = "Testcontainers MySQL 이 필요하므로 Docker 데몬이 가동 중일 때만 실행한다",
)
class WeeklyRankingAggregationJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(WeeklyRankingAggregationJobConfig.JOB_NAME) private val job: Job,
    private val productMetricsDailyJpaRepository: ProductMetricsDailyJpaRepository,
    private val weeklyProductRankJpaRepository: WeeklyProductRankJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        // 2026-04-13 (월) ~ 2026-04-19 (일) 이 대상 주간
        private val MONDAY: LocalDate = LocalDate.of(2026, 4, 13)
        private val WEDNESDAY: LocalDate = LocalDate.of(2026, 4, 15)
        private val SUNDAY: LocalDate = LocalDate.of(2026, 4, 19)
        private val PREV_SUNDAY: LocalDate = LocalDate.of(2026, 4, 12)
        private val NEXT_MONDAY: LocalDate = LocalDate.of(2026, 4, 20)

        @JvmStatic
        fun isDockerAvailable(): Boolean = runCatching {
            val process = ProcessBuilder("docker", "info").redirectErrorStream(true).start()
            process.waitFor() == 0
        }.getOrDefault(false)
    }

    @BeforeEach
    fun beforeEach() {
        jobLauncherTestUtils.job = job
    }

    @AfterEach
    fun afterEach() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("requestDate 파라미터가 없으면 Job 이 FAILED 로 종료된다")
    @Test
    fun failsWithoutRequestDateParameter() {
        val execution = jobLauncherTestUtils.launchJob()

        assertThat(execution.exitStatus.exitCode).isEqualTo(ExitStatus.FAILED.exitCode)
    }

    @DisplayName("product_metrics_daily 7일치를 집계하여 mv_product_rank_weekly 에 score 내림차순으로 적재한다")
    @Test
    fun aggregatesAndInsertsTopRanking() {
        // arrange: 상품 A(좋아요 2 + 조회 3) = 0.2*2 + 0.1*3 = 0.7
        //         상품 B(주문 1) = 0.7*1 = 0.7
        //         상품 C(좋아요 5) = 0.2*5 = 1.0
        seedDaily(productId = 1L, metricDate = MONDAY, likes = 2, views = 3)
        seedDaily(productId = 2L, metricDate = WEDNESDAY, sales = 1)
        seedDaily(productId = 3L, metricDate = SUNDAY, likes = 5)

        // act
        val execution = jobLauncherTestUtils.launchJob(jobParameters(WEDNESDAY))

        // assert
        val rows = weeklyProductRankJpaRepository.findAll().sortedBy { it.rankPosition }
        assertAll(
            { assertThat(execution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(rows).hasSize(3) },
            // 상품 C (1.0) 가 1위
            { assertThat(rows[0].rankPosition).isEqualTo(1) },
            { assertThat(rows[0].productId).isEqualTo(3L) },
            { assertThat(rows[0].score).isEqualTo(1.0) },
            { assertThat(rows[0].periodStart).isEqualTo(MONDAY) },
            { assertThat(rows[0].periodEnd).isEqualTo(SUNDAY) },
            // 상품 A (0.7), 상품 B (0.7) — product_id ASC 로 타이브레이크
            { assertThat(rows[1].rankPosition).isEqualTo(2) },
            { assertThat(rows[1].productId).isEqualTo(1L) },
            { assertThat(rows[2].rankPosition).isEqualTo(3) },
            { assertThat(rows[2].productId).isEqualTo(2L) },
        )
    }

    @DisplayName("같은 requestDate 로 재실행해도 결과가 동일하다 (purge + insert 멱등)")
    @Test
    fun isIdempotentOnRerun() {
        seedDaily(productId = 10L, metricDate = MONDAY, likes = 3)
        seedDaily(productId = 20L, metricDate = SUNDAY, sales = 1)

        // 1차 실행
        val firstExecution = jobLauncherTestUtils.launchJob(jobParameters(MONDAY))
        val firstRows = weeklyProductRankJpaRepository.findAll().sortedBy { it.rankPosition }

        // 2차 실행
        val secondExecution = jobLauncherTestUtils.launchJob(jobParameters(MONDAY))
        val secondRows = weeklyProductRankJpaRepository.findAll().sortedBy { it.rankPosition }

        assertAll(
            { assertThat(firstExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(secondExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(firstRows).hasSize(2) },
            { assertThat(secondRows).hasSize(2) },
            { assertThat(firstRows.map { it.productId to it.rankPosition }).isEqualTo(secondRows.map { it.productId to it.rankPosition }) },
            { assertThat(firstRows.map { it.score }).isEqualTo(secondRows.map { it.score }) },
        )
    }

    @DisplayName("주간 경계 외 데이터(이전 일요일·다음 월요일) 는 집계되지 않는다")
    @Test
    fun ignoresDataOutsideWeek() {
        // 주간 내부
        seedDaily(productId = 100L, metricDate = WEDNESDAY, likes = 1)
        // 주간 외부 (이전 주 일요일, 다음 주 월요일)
        seedDaily(productId = 200L, metricDate = PREV_SUNDAY, sales = 10)
        seedDaily(productId = 300L, metricDate = NEXT_MONDAY, sales = 10)

        jobLauncherTestUtils.launchJob(jobParameters(WEDNESDAY))

        val rows = weeklyProductRankJpaRepository.findAll()
        assertThat(rows).hasSize(1)
        assertThat(rows[0].productId).isEqualTo(100L)
    }

    @DisplayName("요청 주에 데이터가 없으면 0건이 적재된다 (실행은 성공)")
    @Test
    fun emptyWeekProducesNoRows() {
        val execution = jobLauncherTestUtils.launchJob(jobParameters(WEDNESDAY))

        assertAll(
            { assertThat(execution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(weeklyProductRankJpaRepository.findAll()).isEmpty() },
        )
    }

    @DisplayName("동점 상품들은 product_id ASC 로 안정 정렬된다")
    @Test
    fun tieBreakerIsProductIdAscending() {
        // 모두 좋아요 1개씩 — score 동일 (0.2)
        seedDaily(productId = 3L, metricDate = WEDNESDAY, likes = 1)
        seedDaily(productId = 1L, metricDate = WEDNESDAY, likes = 1)
        seedDaily(productId = 2L, metricDate = WEDNESDAY, likes = 1)

        jobLauncherTestUtils.launchJob(jobParameters(WEDNESDAY))

        val rows = weeklyProductRankJpaRepository.findAll().sortedBy { it.rankPosition }
        assertAll(
            { assertThat(rows[0].productId).isEqualTo(1L) },
            { assertThat(rows[1].productId).isEqualTo(2L) },
            { assertThat(rows[2].productId).isEqualTo(3L) },
        )
    }

    private fun seedDaily(
        productId: Long,
        metricDate: LocalDate,
        likes: Long = 0,
        views: Long = 0,
        sales: Long = 0,
    ) {
        productMetricsDailyJpaRepository.save(
            ProductMetricsDailyModel(
                productId = productId,
                metricDate = metricDate,
                likesCount = likes,
                viewsCount = views,
                salesCount = sales,
            ),
        )
    }

    private fun jobParameters(requestDate: LocalDate) = JobParametersBuilder()
        .addString("requestDate", requestDate.toString().replace("-", ""))
        .toJobParameters()
}
