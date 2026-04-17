package com.loopers.job.ranking

import com.loopers.batch.job.ranking.MonthlyRankingJobConfig
import com.loopers.infrastructure.ranking.ProductRankMonthlyJpaRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
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
    @Test
    fun `requestDate 파라미터가 없으면 배치가 실패한다`() {
        jobLauncherTestUtils.job = job

        val jobExecution = jobLauncherTestUtils.launchJob()

        assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.FAILED.exitCode)
    }

    @Test
    fun `product_metrics 데이터를 기반으로 월간 랭킹 TOP 100을 집계한다`() {
        jobLauncherTestUtils.job = job
        insertProductMetrics(productId = 1L, viewCount = 1000, likeCount = 500, orderCount = 200, salesAmount = 5000000)
        insertProductMetrics(productId = 2L, viewCount = 500, likeCount = 300, orderCount = 100, salesAmount = 2000000)
        insertProductMetrics(productId = 3L, viewCount = 2000, likeCount = 100, orderCount = 50, salesAmount = 1000000)

        val requestDate = "20260417"
        val jobParameters = JobParametersBuilder()
            .addString("requestDate", requestDate)
            .toJobParameters()

        val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)

        val expectedDate = LocalDate.of(2026, 4, 1) // 월초
        val rankings = monthlyJpaRepository.findAll().filter { it.rankingDate == expectedDate }

        assertAll(
            { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(rankings).hasSize(3) },
            { assertThat(rankings.sortedBy { it.ranking }.first().productId).isEqualTo(1L) },
            { assertThat(rankings.sortedBy { it.ranking }.first().ranking).isEqualTo(1) },
        )

        cleanUp()
    }

    @Test
    fun `requestDate 기준으로 월의 첫째 날을 ranking_date로 사용한다`() {
        jobLauncherTestUtils.job = job
        insertProductMetrics(productId = 1L, viewCount = 100, likeCount = 50, orderCount = 20, salesAmount = 500000)

        val requestDate = "20260417"
        val jobParameters = JobParametersBuilder()
            .addString("requestDate", requestDate)
            .toJobParameters()

        jobLauncherTestUtils.launchJob(jobParameters)

        val rankings = monthlyJpaRepository.findAll()
        val rankingDate = rankings.first().rankingDate

        assertThat(rankingDate).isEqualTo(LocalDate.of(2026, 4, 1))

        cleanUp()
    }

    @Test
    fun `동일 월에 대해 재실행하면 기존 데이터를 갱신한다`() {
        jobLauncherTestUtils.job = job
        insertProductMetrics(productId = 1L, viewCount = 100, likeCount = 50, orderCount = 20, salesAmount = 500000)

        val jobParameters1 = JobParametersBuilder()
            .addString("requestDate", "20260417")
            .addLong("run.id", 1L)
            .toJobParameters()
        jobLauncherTestUtils.launchJob(jobParameters1)

        val jobParameters2 = JobParametersBuilder()
            .addString("requestDate", "20260417")
            .addLong("run.id", 2L)
            .toJobParameters()
        jobLauncherTestUtils.launchJob(jobParameters2)

        val expectedDate = LocalDate.of(2026, 4, 1)
        val rankings = monthlyJpaRepository.findAll().filter { it.rankingDate == expectedDate }

        assertThat(rankings).hasSize(1)

        cleanUp()
    }

    private fun insertProductMetrics(
        productId: Long,
        viewCount: Long,
        likeCount: Long,
        orderCount: Long,
        salesAmount: Long,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO product_metrics (product_id, view_count, like_count, order_count, sales_amount, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                view_count = VALUES(view_count),
                like_count = VALUES(like_count),
                order_count = VALUES(order_count),
                sales_amount = VALUES(sales_amount),
                updated_at = NOW()
            """.trimIndent(),
            productId,
            viewCount,
            likeCount,
            orderCount,
            salesAmount,
        )
    }

    private fun cleanUp() {
        jdbcTemplate.execute("DELETE FROM mv_product_rank_monthly")
        jdbcTemplate.execute("DELETE FROM product_metrics")
    }
}
