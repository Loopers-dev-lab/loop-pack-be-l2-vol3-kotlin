package com.loopers.job.productmetrics

import com.loopers.batch.job.productmetrics.ProductMetricsSyncJobConfig
import com.loopers.config.redis.RedisConfig
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
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = ["spring.batch.job.name=${ProductMetricsSyncJobConfig.JOB_NAME}"])
class ProductMetricsSyncJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(ProductMetricsSyncJobConfig.JOB_NAME) private val job: Job,
    @param:Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<String, String>,
    private val jdbcTemplate: JdbcTemplate,
) {
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
                view_count BIGINT NOT NULL DEFAULT 0,
                like_count BIGINT NOT NULL DEFAULT 0,
                order_count BIGINT NOT NULL DEFAULT 0,
                created_at DATETIME(6) NOT NULL,
                updated_at DATETIME(6) NOT NULL,
                deleted_at DATETIME(6) NULL,
                UNIQUE KEY uk_product_metrics_product_id (product_id)
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

    private fun seedZSetMetrics(productId: Long, viewCount: Long, likeCount: Long, orderCount: Long, date: LocalDate = TARGET_DATE) {
        val zSet = redisTemplate.opsForZSet()
        val dateKey = date.format(DATE_FORMAT)
        if (viewCount > 0) zSet.incrementScore("$KEY_PREFIX:view:$dateKey", productId.toString(), viewCount.toDouble())
        if (likeCount > 0) zSet.incrementScore("$KEY_PREFIX:like:$dateKey", productId.toString(), likeCount.toDouble())
        if (orderCount > 0) zSet.incrementScore("$KEY_PREFIX:order:$dateKey", productId.toString(), orderCount.toDouble())
    }

    private fun jobParameters(date: LocalDate = TARGET_DATE) = JobParametersBuilder()
        .addLocalDate("requestDate", date)
        .addLong("run.id", System.nanoTime())
        .toJobParameters()

    @Nested
    @DisplayName("productMetricsSyncJob 실행 시")
    inner class Execute {

        @DisplayName("ZSET에 메트릭이 있으면 DB에 동기화된다.")
        @Test
        fun shouldSyncMetricsFromZSetToDb() {
            // arrange
            seedZSetMetrics(PRODUCT_ID_1, viewCount = 100, likeCount = 20, orderCount = 5)
            seedZSetMetrics(PRODUCT_ID_2, viewCount = 50, likeCount = 10, orderCount = 3)

            // act
            val jobExecution = jobLauncherTestUtils.launchJob(jobParameters())

            // assert
            assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)

            val results = jdbcTemplate.queryForList(
                "SELECT product_id, view_count, like_count, order_count FROM product_metrics ORDER BY product_id",
            )

            assertAll(
                { assertThat(results).hasSize(2) },
                { assertThat(results[0]["product_id"]).isEqualTo(PRODUCT_ID_1) },
                { assertThat(results[0]["view_count"]).isEqualTo(100L) },
                { assertThat(results[0]["like_count"]).isEqualTo(20L) },
                { assertThat(results[0]["order_count"]).isEqualTo(5L) },
                { assertThat(results[1]["product_id"]).isEqualTo(PRODUCT_ID_2) },
                { assertThat(results[1]["view_count"]).isEqualTo(50L) },
                { assertThat(results[1]["like_count"]).isEqualTo(10L) },
                { assertThat(results[1]["order_count"]).isEqualTo(3L) },
            )
        }

        @DisplayName("동기화 후에도 Redis ZSET은 변경되지 않는다 (Redis가 SoT).")
        @Test
        fun shouldNotModifyRedisAfterSync() {
            // arrange
            seedZSetMetrics(PRODUCT_ID_1, viewCount = 30, likeCount = 10, orderCount = 2)

            // act
            jobLauncherTestUtils.launchJob(jobParameters())

            // assert
            val zSet = redisTemplate.opsForZSet()
            val dateKey = TARGET_DATE.format(DATE_FORMAT)

            assertAll(
                { assertThat(zSet.score("$KEY_PREFIX:view:$dateKey", PRODUCT_ID_1.toString())!!).isEqualTo(30.0) },
                { assertThat(zSet.score("$KEY_PREFIX:like:$dateKey", PRODUCT_ID_1.toString())!!).isEqualTo(10.0) },
                { assertThat(zSet.score("$KEY_PREFIX:order:$dateKey", PRODUCT_ID_1.toString())!!).isEqualTo(2.0) },
            )
        }

        @DisplayName("2회 동기화 시 DB는 누적이 아닌 스냅샷 덮어쓰기로 갱신된다.")
        @Test
        fun shouldOverwriteOnConsecutiveSyncs() {
            // arrange - 1차
            seedZSetMetrics(PRODUCT_ID_1, viewCount = 10, likeCount = 5, orderCount = 1)
            jobLauncherTestUtils.launchJob(jobParameters())

            // arrange - 2차 (ZSET에 추가 적재 → 누적 상태)
            seedZSetMetrics(PRODUCT_ID_1, viewCount = 20, likeCount = 3, orderCount = 2)

            // act
            jobLauncherTestUtils.launchJob(jobParameters())

            // assert - DB는 ZSET의 현재 누적값(30/8/3)으로 덮어써진다
            val result = jdbcTemplate.queryForMap(
                "SELECT view_count, like_count, order_count FROM product_metrics WHERE product_id = ?",
                PRODUCT_ID_1,
            )

            assertAll(
                { assertThat(result["view_count"]).isEqualTo(30L) },
                { assertThat(result["like_count"]).isEqualTo(8L) },
                { assertThat(result["order_count"]).isEqualTo(3L) },
            )
        }

        @DisplayName("ZSET에 메트릭이 없으면 DB에 변경이 없다.")
        @Test
        fun shouldDoNothingWhenZSetIsEmpty() {
            // act
            val jobExecution = jobLauncherTestUtils.launchJob(jobParameters())

            // assert
            assertAll(
                { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
                {
                    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_metrics", Long::class.java)!!)
                        .isEqualTo(0L)
                },
            )
        }
    }
}
