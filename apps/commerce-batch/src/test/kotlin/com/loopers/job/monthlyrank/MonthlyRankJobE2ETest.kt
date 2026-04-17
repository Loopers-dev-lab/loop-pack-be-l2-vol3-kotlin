package com.loopers.job.monthlyrank

import com.loopers.batch.job.monthlyrank.MonthlyRankJobConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = ["spring.batch.job.name=${MonthlyRankJobConfig.JOB_NAME}"])
class MonthlyRankJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(MonthlyRankJobConfig.JOB_NAME) private val job: Job,
    private val jdbcTemplate: JdbcTemplate,
) {
    companion object {
        private const val PRODUCT_ID_1 = 1L
        private const val PRODUCT_ID_2 = 2L
    }

    @BeforeEach
    fun setUp() {
        jobLauncherTestUtils.job = job
        createTablesIfNotExists()
        cleanUp()
    }

    @AfterEach
    fun tearDown() {
        cleanUp()
    }

    private fun createTablesIfNotExists() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS product_metrics (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL,
                date DATE NOT NULL,
                view_count BIGINT NOT NULL DEFAULT 0,
                like_count BIGINT NOT NULL DEFAULT 0,
                order_count BIGINT NOT NULL DEFAULT 0,
                created_at DATETIME(6) NOT NULL,
                updated_at DATETIME(6) NOT NULL,
                deleted_at DATETIME(6) NULL,
                UNIQUE KEY uk_product_metrics_product_date (product_id, date)
            )
            """.trimIndent(),
        )
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS mv_product_rank_monthly (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL,
                score DOUBLE NOT NULL,
                created_at DATETIME(6) NOT NULL,
                updated_at DATETIME(6) NOT NULL,
                deleted_at DATETIME(6) NULL
            )
            """.trimIndent(),
        )
    }

    private fun cleanUp() {
        jdbcTemplate.execute("DELETE FROM mv_product_rank_monthly")
        jdbcTemplate.execute("DELETE FROM product_metrics")
    }

    private fun seedMetrics(productId: Long, date: LocalDate, viewCount: Long, likeCount: Long, orderCount: Long) {
        jdbcTemplate.update(
            "INSERT INTO product_metrics (product_id, date, view_count, like_count, order_count, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
            productId, date, viewCount, likeCount, orderCount,
        )
    }

    private fun jobParameters() = JobParametersBuilder()
        .addLong("run.id", System.nanoTime())
        .toJobParameters()

    @Nested
    @DisplayName("monthlyRankJob 실행 시")
    inner class Execute {

        @DisplayName("이번 달 일별 메트릭을 합산하여 MV에 적재한다.")
        @Test
        fun shouldAggregateMonthlyMetricsIntoMv() {
            // arrange
            val today = LocalDate.now()
            val firstDay = today.withDayOfMonth(1)

            seedMetrics(PRODUCT_ID_1, firstDay, viewCount = 100, likeCount = 20, orderCount = 5)
            seedMetrics(PRODUCT_ID_2, firstDay, viewCount = 50, likeCount = 10, orderCount = 10)
            if (today != firstDay) {
                seedMetrics(PRODUCT_ID_1, today, viewCount = 200, likeCount = 30, orderCount = 3)
            }

            // act
            val jobExecution = jobLauncherTestUtils.launchJob(jobParameters())

            // assert
            assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)

            val results = jdbcTemplate.queryForList(
                "SELECT product_id, score FROM mv_product_rank_monthly ORDER BY score DESC",
            )
            assertThat(results).hasSizeGreaterThanOrEqualTo(2)
        }

        @DisplayName("지난달 데이터는 집계에 포함되지 않는다.")
        @Test
        fun shouldExcludeDataOutsideMonthRange() {
            // arrange — 지난달 데이터만
            val lastMonth = LocalDate.now().withDayOfMonth(1).minusDays(1)
            seedMetrics(PRODUCT_ID_1, lastMonth, viewCount = 100, likeCount = 20, orderCount = 5)

            // act
            val jobExecution = jobLauncherTestUtils.launchJob(jobParameters())

            // assert
            assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
            val count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mv_product_rank_monthly",
                Long::class.java,
            )
            assertThat(count).isEqualTo(0L)
        }

        @DisplayName("DELETE 후 INSERT로 멱등하게 동작한다.")
        @Test
        fun shouldBeIdempotent() {
            // arrange
            val today = LocalDate.now()
            seedMetrics(PRODUCT_ID_1, today, viewCount = 100, likeCount = 20, orderCount = 5)

            // act — 2번 실행
            jobLauncherTestUtils.launchJob(jobParameters())
            jobLauncherTestUtils.launchJob(jobParameters())

            // assert
            val count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mv_product_rank_monthly",
                Long::class.java,
            )
            assertThat(count).isEqualTo(1L)
        }
    }
}
