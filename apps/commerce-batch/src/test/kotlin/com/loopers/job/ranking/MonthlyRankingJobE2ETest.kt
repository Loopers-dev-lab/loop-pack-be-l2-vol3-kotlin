package com.loopers.job.ranking

import com.loopers.batch.job.ranking.MonthlyRankingJobConfig
import com.loopers.infrastructure.ranking.ProductRankMonthlyJpaRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@SpringBootTest
@SpringBatchTest
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
@TestPropertySource(properties = ["spring.batch.job.name=${MonthlyRankingJobConfig.JOB_NAME}"])
class MonthlyRankingJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(MonthlyRankingJobConfig.JOB_NAME) private val job: Job,
    private val monthlyJpaRepository: ProductRankMonthlyJpaRepository,
    private val jdbcTemplate: JdbcTemplate,
) {
    @AfterEach
    fun cleanUp() {
        jdbcTemplate.execute("DELETE FROM mv_product_rank_monthly")
        jdbcTemplate.execute("DELETE FROM product_metrics")
    }

    @Test
    fun `requestDate 파라미터가 없으면 배치가 실패한다`() {
        jobLauncherTestUtils.job = job

        val jobExecution = jobLauncherTestUtils.launchJob()

        assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.FAILED.exitCode)
    }

    @Test
    fun `해당 월의 product_metrics만 집계한다 (이전 달 데이터는 제외)`() {
        // 대상 월 (2026-04-01 ~ 2026-04-30)
        insertProductMetrics(productId = 1L, bucketDate = EXPECTED_MONTHLY_DATE, viewCount = 1000, likeCount = 500, orderCount = 200)
        insertProductMetrics(productId = 2L, bucketDate = EXPECTED_MONTHLY_DATE.plusDays(15), viewCount = 500, likeCount = 300, orderCount = 100)
        insertProductMetrics(productId = 3L, bucketDate = EXPECTED_MONTHLY_DATE.plusDays(28), viewCount = 2000, likeCount = 100, orderCount = 50)
        // 이전 달 데이터 — 제외되어야 함
        insertProductMetrics(productId = 4L, bucketDate = EXPECTED_MONTHLY_DATE.minusDays(1), viewCount = 9999, likeCount = 9999, orderCount = 9999)
        // 다음 달 데이터 — 제외되어야 함
        insertProductMetrics(productId = 5L, bucketDate = EXPECTED_MONTHLY_DATE.plusMonths(1), viewCount = 9999, likeCount = 9999, orderCount = 9999)

        jobLauncherTestUtils.job = job
        val jobParameters = JobParametersBuilder()
            .addString("requestDate", REQUEST_DATE)
            .toJobParameters()

        val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)

        val rankings = monthlyJpaRepository.findAll()
            .filter { it.rankingDate == EXPECTED_MONTHLY_DATE }
            .sortedBy { it.ranking }

        assertAll(
            { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(rankings).hasSize(3) },
            { assertThat(rankings.map { it.productId }).doesNotContain(4L, 5L) },
            { assertThat(rankings[0].productId).isEqualTo(1L) },
            { assertThat(rankings[0].ranking).isEqualTo(1) },
            { assertThat(rankings[1].productId).isEqualTo(3L) },
            { assertThat(rankings[2].productId).isEqualTo(2L) },
        )
    }

    @Test
    fun `같은 product의 일자별 누적을 합산해서 score를 만든다`() {
        // product 1: SUM(view)=300, SUM(like)=100, SUM(order)=0
        // score = 300*0.1 + 100*0.2 + 0 = 30 + 20 = 50
        insertProductMetrics(productId = 1L, bucketDate = EXPECTED_MONTHLY_DATE, viewCount = 100, likeCount = 50, orderCount = 0)
        insertProductMetrics(productId = 1L, bucketDate = EXPECTED_MONTHLY_DATE.plusDays(10), viewCount = 200, likeCount = 50, orderCount = 0)

        jobLauncherTestUtils.job = job
        val jobParameters = JobParametersBuilder()
            .addString("requestDate", REQUEST_DATE)
            .toJobParameters()

        jobLauncherTestUtils.launchJob(jobParameters)

        val rankings = monthlyJpaRepository.findAll().filter { it.rankingDate == EXPECTED_MONTHLY_DATE }

        assertAll(
            { assertThat(rankings).hasSize(1) },
            { assertThat(rankings[0].productId).isEqualTo(1L) },
            { assertThat(rankings[0].score).isEqualTo(50.0) },
        )
    }

    @Test
    fun `requestDate 기준으로 월의 첫째 날을 ranking_date로 사용한다`() {
        insertProductMetrics(productId = 1L, bucketDate = EXPECTED_MONTHLY_DATE.plusDays(5), viewCount = 100, likeCount = 50, orderCount = 20)

        jobLauncherTestUtils.job = job
        val jobParameters = JobParametersBuilder()
            .addString("requestDate", REQUEST_DATE)
            .toJobParameters()

        jobLauncherTestUtils.launchJob(jobParameters)

        val rankings = monthlyJpaRepository.findAll()
            .filter { it.rankingDate == EXPECTED_MONTHLY_DATE }

        assertThat(rankings).isNotEmpty
        assertThat(rankings.first().rankingDate).isEqualTo(EXPECTED_MONTHLY_DATE)
    }

    @Test
    fun `동일 월에 대해 재실행하면 기존 데이터를 갱신한다`() {
        insertProductMetrics(productId = 1L, bucketDate = EXPECTED_MONTHLY_DATE, viewCount = 100, likeCount = 50, orderCount = 20)

        jobLauncherTestUtils.job = job
        val jobParameters1 = JobParametersBuilder()
            .addString("requestDate", REQUEST_DATE)
            .addLong("run.id", 1L)
            .toJobParameters()
        jobLauncherTestUtils.launchJob(jobParameters1)

        // 같은 product/bucket_date에 다시 insert는 unique 제약 위반이므로
        // 다른 일자에 추가 데이터를 넣어 재실행 갱신을 검증한다
        insertProductMetrics(productId = 1L, bucketDate = EXPECTED_MONTHLY_DATE.plusDays(7), viewCount = 200, likeCount = 100, orderCount = 40)

        val jobParameters2 = JobParametersBuilder()
            .addString("requestDate", REQUEST_DATE)
            .addLong("run.id", 2L)
            .toJobParameters()
        jobLauncherTestUtils.launchJob(jobParameters2)

        val rankings = monthlyJpaRepository.findAll().filter { it.rankingDate == EXPECTED_MONTHLY_DATE }

        assertThat(rankings).hasSize(1)
        assertThat(rankings.first().score).isGreaterThan(0.0)
    }

    private fun insertProductMetrics(
        productId: Long,
        bucketDate: LocalDate,
        viewCount: Long,
        likeCount: Long,
        orderCount: Long,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO product_metrics (product_id, bucket_date, view_count, like_count, order_count, sales_amount, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 0, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                view_count = VALUES(view_count),
                like_count = VALUES(like_count),
                order_count = VALUES(order_count),
                updated_at = NOW()
            """.trimIndent(),
            productId,
            java.sql.Date.valueOf(bucketDate),
            viewCount,
            likeCount,
            orderCount,
        )
    }

    companion object {
        private const val REQUEST_DATE = "20260417"
        private val EXPECTED_MONTHLY_DATE = LocalDate.of(2026, 4, 1)
    }
}
