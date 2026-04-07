package com.loopers.job.productmetrics

import com.loopers.batch.job.productmetrics.ProductMetricsSyncJobConfig
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
import com.loopers.config.redis.RedisConfig
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

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
        private const val KEY_PREFIX = "product:metrics"
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
        redisTemplate.delete("$KEY_PREFIX:$PRODUCT_ID_1")
        redisTemplate.delete("$KEY_PREFIX:$PRODUCT_ID_2")
        jdbcTemplate.execute("DELETE FROM product_metrics")
    }

    private fun seedRedisMetrics(productId: Long, viewCount: Long, likeCount: Long, orderCount: Long) {
        val ops = redisTemplate.opsForHash<String, String>()
        val key = "$KEY_PREFIX:$productId"
        ops.increment(key, "viewCount", viewCount)
        ops.increment(key, "likeCount", likeCount)
        ops.increment(key, "orderCount", orderCount)
    }

    private fun uniqueJobParameters() = JobParametersBuilder()
        .addLocalDate("requestDate", LocalDate.now())
        .addLong("run.id", System.nanoTime())
        .toJobParameters()

    @Nested
    @DisplayName("productMetricsSyncJob 실행 시")
    inner class Execute {

        @DisplayName("Redis에 메트릭이 있으면 DB에 동기화된다.")
        @Test
        fun shouldSyncMetricsFromRedisToDb() {
            // arrange
            seedRedisMetrics(PRODUCT_ID_1, viewCount = 100, likeCount = 20, orderCount = 5)
            seedRedisMetrics(PRODUCT_ID_2, viewCount = 50, likeCount = 10, orderCount = 3)

            // act
            val jobExecution = jobLauncherTestUtils.launchJob(uniqueJobParameters())

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

        @DisplayName("동기화 후 Redis 카운터는 0으로 차감된다.")
        @Test
        fun shouldResetRedisCountersAfterSync() {
            // arrange
            seedRedisMetrics(PRODUCT_ID_1, viewCount = 30, likeCount = 10, orderCount = 2)

            // act
            jobLauncherTestUtils.launchJob(uniqueJobParameters())

            // assert
            val ops = redisTemplate.opsForHash<String, String>()
            val key = "$KEY_PREFIX:$PRODUCT_ID_1"

            assertAll(
                { assertThat(ops.get(key, "viewCount")).isEqualTo("0") },
                { assertThat(ops.get(key, "likeCount")).isEqualTo("0") },
                { assertThat(ops.get(key, "orderCount")).isEqualTo("0") },
            )
        }

        @DisplayName("2회 동기화 시 DB에 누적 합산된다.")
        @Test
        fun shouldAccumulateOnConsecutiveSyncs() {
            // arrange - 1차
            seedRedisMetrics(PRODUCT_ID_1, viewCount = 10, likeCount = 5, orderCount = 1)
            jobLauncherTestUtils.launchJob(uniqueJobParameters())

            // arrange - 2차
            seedRedisMetrics(PRODUCT_ID_1, viewCount = 20, likeCount = 3, orderCount = 2)

            // act
            jobLauncherTestUtils.launchJob(uniqueJobParameters())

            // assert
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

        @DisplayName("Redis에 메트릭이 없으면 DB에 변경이 없다.")
        @Test
        fun shouldDoNothingWhenRedisIsEmpty() {
            // act
            val jobExecution = jobLauncherTestUtils.launchJob(uniqueJobParameters())

            // assert
            assertAll(
                { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
                {
                    assertThat(
                        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_metrics", Long::class.java),
                    ).isEqualTo(0L)
                },
            )
        }
    }
}
