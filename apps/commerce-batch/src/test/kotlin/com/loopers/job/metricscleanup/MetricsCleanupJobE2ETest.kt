package com.loopers.job.metricscleanup

import com.loopers.batch.job.metricscleanup.MetricsCleanupJobConfig
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
@TestPropertySource(properties = ["spring.batch.job.name=${MetricsCleanupJobConfig.JOB_NAME}"])
class MetricsCleanupJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(MetricsCleanupJobConfig.JOB_NAME) private val job: Job,
    private val jdbcTemplate: JdbcTemplate,
) {
    companion object {
        private const val PRODUCT_ID_1 = 1L
        private const val PRODUCT_ID_2 = 2L
    }

    @BeforeEach
    fun setUp() {
        jobLauncherTestUtils.job = job
        createTableIfNotExists()
        cleanUp()
    }

    @AfterEach
    fun tearDown() {
        cleanUp()
    }

    private fun createTableIfNotExists() {
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
    }

    private fun cleanUp() {
        jdbcTemplate.execute("DELETE FROM product_metrics")
    }

    private fun seedMetrics(productId: Long, date: LocalDate) {
        jdbcTemplate.update(
            "INSERT INTO product_metrics (product_id, date, view_count, like_count, order_count, created_at, updated_at) " +
                "VALUES (?, ?, 10, 5, 1, NOW(), NOW())",
            productId, date,
        )
    }

    private fun jobParameters() = JobParametersBuilder()
        .addLong("run.id", System.nanoTime())
        .toJobParameters()

    @Nested
    @DisplayName("metricsCleanupJob 실행 시")
    inner class Execute {

        @DisplayName("60일 이전 데이터는 삭제하고, 최근 데이터는 보존한다.")
        @Test
        fun shouldDeleteOldDataAndPreserveRecent() {
            // arrange
            val oldDate = LocalDate.now().minusDays(61)
            val recentDate = LocalDate.now().minusDays(30)

            seedMetrics(PRODUCT_ID_1, oldDate)
            seedMetrics(PRODUCT_ID_2, recentDate)

            // act
            val jobExecution = jobLauncherTestUtils.launchJob(jobParameters())

            // assert
            assertAll(
                { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
                {
                    val count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM product_metrics",
                        Long::class.java,
                    )
                    assertThat(count).isEqualTo(1L)
                },
                {
                    val remaining = jdbcTemplate.queryForObject(
                        "SELECT product_id FROM product_metrics",
                        Long::class.java,
                    )
                    assertThat(remaining).isEqualTo(PRODUCT_ID_2)
                },
            )
        }

        @DisplayName("삭제할 데이터가 없으면 정상 완료된다.")
        @Test
        fun shouldCompleteWhenNothingToDelete() {
            // arrange — 최근 데이터만
            seedMetrics(PRODUCT_ID_1, LocalDate.now())

            // act
            val jobExecution = jobLauncherTestUtils.launchJob(jobParameters())

            // assert
            assertAll(
                { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
                {
                    val count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM product_metrics",
                        Long::class.java,
                    )
                    assertThat(count).isEqualTo(1L)
                },
            )
        }
    }
}
