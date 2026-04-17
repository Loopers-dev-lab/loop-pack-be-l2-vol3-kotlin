package com.loopers.batch.job.ranking

import com.loopers.domain.metrics.ProductMetricsDailyModel
import com.loopers.infrastructure.metrics.ProductMetricsDailyJpaRepository
import com.loopers.infrastructure.mv.MonthlyProductRankJpaRepository
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
 * 월간 랭킹 집계 Job E2E.
 *
 * Phase 3 의 주간 E2E 패턴을 복제하여 월 경계·yearMonthVal 기반 검증을 추가한다.
 */
@SpringBootTest
@SpringBatchTest
@Import(MySqlTestContainersConfig::class)
@TestPropertySource(properties = ["spring.batch.job.name=${MonthlyRankingAggregationJobConfig.JOB_NAME}"])
@EnabledIf(
    value = "com.loopers.batch.job.ranking.MonthlyRankingAggregationJobE2ETest#isDockerAvailable",
    disabledReason = "Testcontainers MySQL 이 필요하므로 Docker 데몬이 가동 중일 때만 실행한다",
)
class MonthlyRankingAggregationJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(MonthlyRankingAggregationJobConfig.JOB_NAME) private val job: Job,
    private val productMetricsDailyJpaRepository: ProductMetricsDailyJpaRepository,
    private val monthlyProductRankJpaRepository: MonthlyProductRankJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        // 2026-04-01 ~ 2026-04-30 이 대상 월
        private val FIRST_DAY: LocalDate = LocalDate.of(2026, 4, 1)
        private val MID_MONTH: LocalDate = LocalDate.of(2026, 4, 15)
        private val LAST_DAY: LocalDate = LocalDate.of(2026, 4, 30)
        private val PREV_MONTH_LAST: LocalDate = LocalDate.of(2026, 3, 31)
        private val NEXT_MONTH_FIRST: LocalDate = LocalDate.of(2026, 5, 1)

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

    @DisplayName("해당 월의 product_metrics_daily 를 집계해 mv_product_rank_monthly 에 score 내림차순으로 적재한다")
    @Test
    fun aggregatesAndInsertsTopRanking() {
        // 상품 1: 좋아요 2 + 조회 3 = 0.7
        // 상품 2: 주문 1 = 0.7
        // 상품 3: 좋아요 5 = 1.0
        seedDaily(productId = 1L, metricDate = FIRST_DAY, likes = 2, views = 3)
        seedDaily(productId = 2L, metricDate = MID_MONTH, sales = 1)
        seedDaily(productId = 3L, metricDate = LAST_DAY, likes = 5)

        val execution = jobLauncherTestUtils.launchJob(jobParameters(MID_MONTH))

        val rows = monthlyProductRankJpaRepository.findAll().sortedBy { it.rankPosition }
        assertAll(
            { assertThat(execution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(rows).hasSize(3) },
            { assertThat(rows[0].yearMonthVal).isEqualTo("2026-04") },
            { assertThat(rows[0].periodStart).isEqualTo(FIRST_DAY) },
            { assertThat(rows[0].periodEnd).isEqualTo(LAST_DAY) },
            // 1위: 상품 3 (1.0)
            { assertThat(rows[0].rankPosition).isEqualTo(1) },
            { assertThat(rows[0].productId).isEqualTo(3L) },
            { assertThat(rows[0].score).isEqualTo(1.0) },
            // 2~3위: 상품 1, 2 (각 0.7, product_id ASC 타이브레이크)
            { assertThat(rows[1].productId).isEqualTo(1L) },
            { assertThat(rows[2].productId).isEqualTo(2L) },
        )
    }

    @DisplayName("같은 requestDate 로 재실행해도 결과가 동일하다 (purge + insert 멱등)")
    @Test
    fun isIdempotentOnRerun() {
        seedDaily(productId = 10L, metricDate = FIRST_DAY, likes = 3)
        seedDaily(productId = 20L, metricDate = LAST_DAY, sales = 1)

        val firstExecution = jobLauncherTestUtils.launchJob(jobParameters(MID_MONTH))
        val firstRows = monthlyProductRankJpaRepository.findAll().sortedBy { it.rankPosition }
        val secondExecution = jobLauncherTestUtils.launchJob(jobParameters(MID_MONTH))
        val secondRows = monthlyProductRankJpaRepository.findAll().sortedBy { it.rankPosition }

        assertAll(
            { assertThat(firstExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(secondExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(firstRows.map { it.productId to it.rankPosition }).isEqualTo(secondRows.map { it.productId to it.rankPosition }) },
            { assertThat(firstRows.map { it.score }).isEqualTo(secondRows.map { it.score }) },
        )
    }

    @DisplayName("전월 말일과 익월 1일 데이터는 집계되지 않는다")
    @Test
    fun ignoresDataOutsideMonth() {
        seedDaily(productId = 100L, metricDate = MID_MONTH, likes = 1)
        seedDaily(productId = 200L, metricDate = PREV_MONTH_LAST, sales = 10)
        seedDaily(productId = 300L, metricDate = NEXT_MONTH_FIRST, sales = 10)

        jobLauncherTestUtils.launchJob(jobParameters(MID_MONTH))

        val rows = monthlyProductRankJpaRepository.findAll()
        assertThat(rows).hasSize(1)
        assertThat(rows[0].productId).isEqualTo(100L)
    }

    @DisplayName("윤년 2월에도 29일 데이터까지 정상 집계된다")
    @Test
    fun leapFebruaryIncludesFeb29() {
        val feb29Date = LocalDate.of(2024, 2, 29)
        val feb10Date = LocalDate.of(2024, 2, 10)
        val mar1Date = LocalDate.of(2024, 3, 1)

        seedDaily(productId = 1L, metricDate = feb10Date, likes = 1)
        seedDaily(productId = 2L, metricDate = feb29Date, likes = 2)
        // 3월 데이터는 포함되면 안 됨
        seedDaily(productId = 3L, metricDate = mar1Date, sales = 10)

        jobLauncherTestUtils.launchJob(jobParameters(feb10Date))

        val rows = monthlyProductRankJpaRepository.findAll().sortedBy { it.rankPosition }
        assertAll(
            { assertThat(rows).hasSize(2) },
            { assertThat(rows.map { it.productId }).containsExactlyInAnyOrder(1L, 2L) },
            { assertThat(rows[0].yearMonthVal).isEqualTo("2024-02") },
            { assertThat(rows[0].periodEnd).isEqualTo(feb29Date) },
        )
    }

    @DisplayName("요청 월에 데이터가 없으면 0건 적재 + 성공 종료")
    @Test
    fun emptyMonthProducesNoRows() {
        val execution = jobLauncherTestUtils.launchJob(jobParameters(MID_MONTH))

        assertAll(
            { assertThat(execution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(monthlyProductRankJpaRepository.findAll()).isEmpty() },
        )
    }

    @DisplayName("동점 상품들은 product_id ASC 로 안정 정렬된다")
    @Test
    fun tieBreakerIsProductIdAscending() {
        seedDaily(productId = 3L, metricDate = MID_MONTH, likes = 1)
        seedDaily(productId = 1L, metricDate = MID_MONTH, likes = 1)
        seedDaily(productId = 2L, metricDate = MID_MONTH, likes = 1)

        jobLauncherTestUtils.launchJob(jobParameters(MID_MONTH))

        val rows = monthlyProductRankJpaRepository.findAll().sortedBy { it.rankPosition }
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
