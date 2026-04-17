package com.loopers.job.productmetrics

import com.loopers.batch.job.productmetrics.ProductMetricsSyncJobConfig
import com.loopers.config.redis.RedisConfig
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * UPSERT 실패 시 Job이 FAILED로 종료되고 기존 DB 행이 보존되는지 검증한다.
 */
@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = ["spring.batch.job.name=${ProductMetricsSyncJobConfig.JOB_NAME}"])
class ProductMetricsSyncJobRollbackTest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(ProductMetricsSyncJobConfig.JOB_NAME) private val job: Job,
    @param:Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<String, String>,
    private val jdbcTemplate: JdbcTemplate,
) {
    @SpykBean
    private lateinit var spyJdbcTemplate: JdbcTemplate

    companion object {
        private const val KEY_PREFIX = "rank"
        private const val PRODUCT_ID_1 = 1L
        private const val PRODUCT_ID_2 = 2L
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val TARGET_DATE: LocalDate = LocalDate.of(2026, 4, 7)
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
        listOf("view", "like", "order", "all").forEach { type ->
            redisTemplate.delete("$KEY_PREFIX:$type:${TARGET_DATE.format(DATE_FORMAT)}")
        }
        jdbcTemplate.execute("DELETE FROM product_metrics")
    }

    private fun seedZSetMetrics(productId: Long, viewCount: Long) {
        val zSet = redisTemplate.opsForZSet()
        val dateKey = TARGET_DATE.format(DATE_FORMAT)
        zSet.incrementScore("$KEY_PREFIX:view:$dateKey", productId.toString(), viewCount.toDouble())
    }

    @DisplayName("UPSERT 실패 시 Job이 FAILED로 종료되고 기존 DB 행이 보존된다.")
    @Test
    fun shouldPreserveExistingDataWhenUpsertFails() {
        // arrange — DB에 기존 행 직접 삽입
        jdbcTemplate.update(
            "INSERT INTO product_metrics (product_id, date, view_count, like_count, order_count, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
            PRODUCT_ID_1, TARGET_DATE, 100L, 20L, 5L,
        )

        // arrange — ZSET에 새 데이터 적재 (정상적인 snapshot)
        seedZSetMetrics(PRODUCT_ID_2, viewCount = 50)

        // arrange — product_metrics에 대한 batchUpdate(UPSERT)만 실패시킴
        every {
            spyJdbcTemplate.batchUpdate(
                match<String> { it.contains("product_metrics") && it.contains("INSERT") },
                any<List<Array<Any>>>(),
            )
        } throws RuntimeException("simulated UPSERT failure")

        // act
        val jobExecution = jobLauncherTestUtils.launchJob(jobParameters())

        // assert
        assertAll(
            // Job은 실패로 종료
            { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.FAILED.exitCode) },
            // 기존 행은 그대로 보존되어야 함
            {
                val result = jdbcTemplate.queryForMap(
                    "SELECT view_count, like_count, order_count FROM product_metrics WHERE product_id = ? AND date = ?",
                    PRODUCT_ID_1, TARGET_DATE,
                )
                assertAll(
                    { assertThat(result["view_count"]).isEqualTo(100L) },
                    { assertThat(result["like_count"]).isEqualTo(20L) },
                    { assertThat(result["order_count"]).isEqualTo(5L) },
                )
            },
        )
    }

    private fun jobParameters() = JobParametersBuilder()
        .addLocalDate("requestDate", TARGET_DATE)
        .addLong("run.id", System.nanoTime())
        .toJobParameters()
}
