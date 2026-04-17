package com.loopers.job.ranking

import com.loopers.batch.job.ranking.WeeklyRankingJobConfig
import com.loopers.infrastructure.ranking.ProductRankWeeklyJpaRepository
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
import java.time.DayOfWeek
import java.time.LocalDate

@SpringBootTest
@SpringBatchTest
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
@TestPropertySource(properties = ["spring.batch.job.name=${WeeklyRankingJobConfig.JOB_NAME}"])
class WeeklyRankingJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(WeeklyRankingJobConfig.JOB_NAME) private val job: Job,
    private val weeklyJpaRepository: ProductRankWeeklyJpaRepository,
    private val jdbcTemplate: JdbcTemplate,
) {
    @AfterEach
    fun cleanUp() {
        jdbcTemplate.execute("DELETE FROM mv_product_rank_weekly")
        jdbcTemplate.execute("DELETE FROM product_metrics")
    }

    @Test
    fun `requestDate 파라미터가 없으면 배치가 실패한다`() {
        jobLauncherTestUtils.job = job

        val jobExecution = jobLauncherTestUtils.launchJob()

        assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.FAILED.exitCode)
    }

    @Test
    fun `product_metrics 데이터를 기반으로 주간 랭킹 TOP 100을 집계한다`() {
        // score: 1000*0.1 + 500*0.2 + 200*0.7 = 340
        insertProductMetrics(productId = 1L, viewCount = 1000, likeCount = 500, orderCount = 200)
        // score: 500*0.1 + 300*0.2 + 100*0.7 = 180
        insertProductMetrics(productId = 2L, viewCount = 500, likeCount = 300, orderCount = 100)
        // score: 2000*0.1 + 100*0.2 + 50*0.7 = 255
        insertProductMetrics(productId = 3L, viewCount = 2000, likeCount = 100, orderCount = 50)

        jobLauncherTestUtils.job = job
        val jobParameters = JobParametersBuilder()
            .addString("requestDate", REQUEST_DATE)
            .toJobParameters()

        val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)

        val rankings = weeklyJpaRepository.findAll()
            .filter { it.rankingDate == EXPECTED_WEEKLY_DATE }
            .sortedBy { it.ranking }

        assertAll(
            { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(rankings).hasSize(3) },
            { assertThat(rankings[0].productId).isEqualTo(1L) },
            { assertThat(rankings[0].ranking).isEqualTo(1) },
            { assertThat(rankings[1].productId).isEqualTo(3L) },
            { assertThat(rankings[2].productId).isEqualTo(2L) },
        )
    }

    @Test
    fun `동일 주차에 대해 재실행하면 기존 데이터를 갱신한다`() {
        insertProductMetrics(productId = 1L, viewCount = 100, likeCount = 50, orderCount = 20)

        jobLauncherTestUtils.job = job
        val jobParameters1 = JobParametersBuilder()
            .addString("requestDate", REQUEST_DATE)
            .addLong("run.id", 1L)
            .toJobParameters()
        jobLauncherTestUtils.launchJob(jobParameters1)

        val jobParameters2 = JobParametersBuilder()
            .addString("requestDate", REQUEST_DATE)
            .addLong("run.id", 2L)
            .toJobParameters()
        jobLauncherTestUtils.launchJob(jobParameters2)

        val rankings = weeklyJpaRepository.findAll().filter { it.rankingDate == EXPECTED_WEEKLY_DATE }

        assertThat(rankings).hasSize(1)
    }

    @Test
    fun `requestDate 기준으로 주의 시작일(월요일)을 ranking_date로 사용한다`() {
        insertProductMetrics(productId = 1L, viewCount = 100, likeCount = 50, orderCount = 20)

        jobLauncherTestUtils.job = job
        val jobParameters = JobParametersBuilder()
            .addString("requestDate", "20260415")
            .toJobParameters()

        jobLauncherTestUtils.launchJob(jobParameters)

        val rankings = weeklyJpaRepository.findAll()
            .filter { it.rankingDate == EXPECTED_WEEKLY_DATE }

        assertThat(rankings).isNotEmpty
        assertThat(rankings.first().rankingDate).isEqualTo(EXPECTED_WEEKLY_DATE)
        assertThat(rankings.first().rankingDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
    }

    private fun insertProductMetrics(
        productId: Long,
        viewCount: Long,
        likeCount: Long,
        orderCount: Long,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO product_metrics (product_id, view_count, like_count, order_count, sales_amount, created_at, updated_at)
            VALUES (?, ?, ?, ?, 0, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                view_count = VALUES(view_count),
                like_count = VALUES(like_count),
                order_count = VALUES(order_count),
                updated_at = NOW()
            """.trimIndent(),
            productId,
            viewCount,
            likeCount,
            orderCount,
        )
    }

    companion object {
        private const val REQUEST_DATE = "20260417"
        private val EXPECTED_WEEKLY_DATE = LocalDate.of(2026, 4, 13)
    }
}
