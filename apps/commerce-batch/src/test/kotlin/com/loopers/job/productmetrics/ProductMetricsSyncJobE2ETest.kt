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

    private fun cleanUpZSets() {
        val dates = listOf(TARGET_DATE, TARGET_DATE.minusDays(1))
        dates.forEach { date ->
            listOf("view", "like", "order", "all").forEach { type ->
                redisTemplate.delete("$KEY_PREFIX:$type:${date.format(DATE_FORMAT)}")
            }
        }
    }

    private fun cleanUp() {
        cleanUpZSets()
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
                "SELECT view_count, like_count, order_count FROM product_metrics WHERE product_id = ? AND date = ?",
                PRODUCT_ID_1, TARGET_DATE,
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

        @DisplayName("빈 스냅샷이어도 기존 DB 행을 wipe하지 않는다.")
        @Test
        fun shouldNotWipeDbWhenZSetIsEmpty() {
            // arrange — DB에 기존 행 직접 삽입 (이전 동기화의 잔존 가정)
            jdbcTemplate.update(
                "INSERT INTO product_metrics (product_id, date, view_count, like_count, order_count, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                PRODUCT_ID_1, TARGET_DATE, 100L, 20L, 5L,
            )

            // act — ZSET 비어있는 상태로 실행
            jobLauncherTestUtils.launchJob(jobParameters())

            // assert — 기존 행 보존
            val result = jdbcTemplate.queryForMap(
                "SELECT view_count, like_count, order_count FROM product_metrics WHERE product_id = ? AND date = ?",
                PRODUCT_ID_1, TARGET_DATE,
            )
            assertAll(
                { assertThat(result["view_count"]).isEqualTo(100L) },
                { assertThat(result["like_count"]).isEqualTo(20L) },
                { assertThat(result["order_count"]).isEqualTo(5L) },
            )
        }

        @DisplayName("다른 날짜의 데이터는 영향받지 않는다.")
        @Test
        fun shouldNotAffectOtherDateData() {
            // arrange — 이전 날짜에 데이터 적재
            val previousDate = TARGET_DATE.minusDays(1)
            seedZSetMetrics(PRODUCT_ID_1, viewCount = 100, likeCount = 20, orderCount = 5, date = previousDate)
            jobLauncherTestUtils.launchJob(jobParameters(previousDate))

            // arrange — 오늘 날짜에 다른 데이터 적재
            seedZSetMetrics(PRODUCT_ID_1, viewCount = 200, likeCount = 30, orderCount = 7)

            // act
            jobLauncherTestUtils.launchJob(jobParameters())

            // assert — 이전 날짜 데이터는 보존, 오늘 날짜 데이터는 별도 row
            val results = jdbcTemplate.queryForList(
                "SELECT product_id, date, view_count, like_count, order_count FROM product_metrics WHERE product_id = ? ORDER BY date",
                PRODUCT_ID_1,
            )
            assertAll(
                { assertThat(results).hasSize(2) },
                { assertThat(results[0]["date"].toString()).isEqualTo(previousDate.toString()) },
                { assertThat(results[0]["view_count"]).isEqualTo(100L) },
                { assertThat(results[1]["date"].toString()).isEqualTo(TARGET_DATE.toString()) },
                { assertThat(results[1]["view_count"]).isEqualTo(200L) },
            )
        }

        @DisplayName("requestDate를 yyyy-MM-dd 문자열로 전달해도 정상 동작한다.")
        @Test
        fun shouldAcceptRequestDateAsString() {
            // arrange
            seedZSetMetrics(PRODUCT_ID_1, viewCount = 10, likeCount = 5, orderCount = 1)
            val params = JobParametersBuilder()
                .addString("requestDate", TARGET_DATE.toString())
                .addLong("run.id", System.nanoTime())
                .toJobParameters()

            // act
            val jobExecution = jobLauncherTestUtils.launchJob(params)

            // assert
            assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
            val result = jdbcTemplate.queryForMap(
                "SELECT view_count FROM product_metrics WHERE product_id = ?",
                PRODUCT_ID_1,
            )
            assertThat(result["view_count"]).isEqualTo(10L)
        }

        @DisplayName("requestDate 형식이 잘못되면 Job이 실패한다.")
        @Test
        fun shouldFailJobWhenRequestDateInvalid() {
            // arrange
            val params = JobParametersBuilder()
                .addString("requestDate", "not-a-date")
                .addLong("run.id", System.nanoTime())
                .toJobParameters()

            // act
            val jobExecution = jobLauncherTestUtils.launchJob(params)

            // assert
            assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.FAILED.exitCode)
        }
    }
}
