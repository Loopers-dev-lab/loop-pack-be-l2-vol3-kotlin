package com.loopers.job.ranking

import com.loopers.batch.job.ranking.RankingAggregationJobConfig
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest
@SpringBatchTest
@TestPropertySource(
    properties = [
        "spring.batch.job.name=${RankingAggregationJobConfig.JOB_NAME}",
    ],
)
class RankingAggregationJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(RankingAggregationJobConfig.JOB_NAME) private val job: Job,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    private val runIdSeq = AtomicLong(System.currentTimeMillis())

    @BeforeEach
    fun setUp() {
        jobLauncherTestUtils.job = job
        createSourceTablesIfNotExist()
        cleanUpTables()
    }

    private fun uniqueJobParams(requestDate: String) =
        JobParametersBuilder()
            .addString("requestDate", requestDate)
            .addLong("run.id", runIdSeq.incrementAndGet())
            .toJobParameters()

    @Nested
    @DisplayName("Job 실행 검증")
    inner class JobExecution {

        @DisplayName("requestDate 파라미터가 없으면 Job이 실패한다")
        @Test
        fun shouldFail_whenRequestDateMissing() {
            // arrange & act
            val jobExecution = jobLauncherTestUtils.launchJob()

            // assert
            assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.FAILED.exitCode)
        }

        @DisplayName("ranking_event_log 데이터가 있으면 주간/월간 MV가 동시에 생성된다")
        @Test
        fun shouldCreateWeeklyAndMonthlyMv_whenEventLogExists() {
            // arrange
            insertScoreConfig()
            insertEventLogs(LocalDate.of(2026, 4, 13))
            insertEventLogs(LocalDate.of(2026, 4, 14))
            insertEventLogs(LocalDate.of(2026, 4, 15))

            // act
            val jobExecution = jobLauncherTestUtils.launchJob(uniqueJobParams("2026-04-15"))

            // assert
            assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)

            val weeklyResults = queryMvWeekly(LocalDate.of(2026, 4, 13))
            val monthlyResults = queryMvMonthly(LocalDate.of(2026, 4, 1))

            assertAll(
                { assertThat(weeklyResults).isNotEmpty() },
                { assertThat(weeklyResults.first()["rank"]).isEqualTo(1) },
                { assertThat(monthlyResults).isNotEmpty() },
                { assertThat(monthlyResults.first()["rank"]).isEqualTo(1) },
            )
        }

        @DisplayName("동일 requestDate로 2회 실행해도 데이터 정합성이 유지된다 (멱등성)")
        @Test
        fun shouldBeIdempotent_whenExecutedTwice() {
            // arrange
            insertScoreConfig()
            insertEventLogs(LocalDate.of(2026, 4, 13))

            // act
            jobLauncherTestUtils.launchJob(uniqueJobParams("2026-04-13"))
            val jobExecution2 = jobLauncherTestUtils.launchJob(uniqueJobParams("2026-04-13"))

            // assert
            assertThat(jobExecution2.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)

            val weeklyResults = queryMvWeekly(LocalDate.of(2026, 4, 13))
            assertThat(weeklyResults).hasSize(2)
        }

        @DisplayName("이벤트 로그가 없으면 MV 테이블이 비어있다")
        @Test
        fun shouldProduceEmptyMv_whenNoEventLogs() {
            // arrange
            insertScoreConfig()

            // act
            val jobExecution = jobLauncherTestUtils.launchJob(uniqueJobParams("2026-03-10"))

            // assert
            assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)

            val weeklyResults = queryMvWeekly(LocalDate.of(2026, 3, 9))
            assertThat(weeklyResults).isEmpty()
        }
    }

    private fun createSourceTablesIfNotExist() {
        jdbcTemplate.jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS ranking_event_log (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL,
                event_type VARCHAR(20) NOT NULL,
                event_value DOUBLE NOT NULL,
                occurred_date DATE NOT NULL,
                event_id VARCHAR(255) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_ranking_event_log_date_product (occurred_date, product_id)
            )
            """,
        )
        jdbcTemplate.jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS ranking_score_config (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                config_key VARCHAR(255) NOT NULL UNIQUE,
                config_value DOUBLE NOT NULL,
                description VARCHAR(255),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """,
        )
        jdbcTemplate.jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS mv_product_rank_weekly (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL,
                total_score DOUBLE NOT NULL,
                view_count INT NOT NULL DEFAULT 0,
                like_count INT NOT NULL DEFAULT 0,
                order_count INT NOT NULL DEFAULT 0,
                `rank` INT NOT NULL,
                period_start_date DATE NOT NULL,
                period_end_date DATE NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_mv_weekly_period_rank (period_start_date, `rank`),
                UNIQUE KEY uk_mv_weekly_product_period (product_id, period_start_date)
            )
            """,
        )
        jdbcTemplate.jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS mv_product_rank_monthly (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL,
                total_score DOUBLE NOT NULL,
                view_count INT NOT NULL DEFAULT 0,
                like_count INT NOT NULL DEFAULT 0,
                order_count INT NOT NULL DEFAULT 0,
                `rank` INT NOT NULL,
                period_start_date DATE NOT NULL,
                period_end_date DATE NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_mv_monthly_period_rank (period_start_date, `rank`),
                UNIQUE KEY uk_mv_monthly_product_period (product_id, period_start_date)
            )
            """,
        )
    }

    private fun cleanUpTables() {
        jdbcTemplate.jdbcTemplate.execute("DELETE FROM ranking_event_log")
        jdbcTemplate.jdbcTemplate.execute("DELETE FROM ranking_score_config")
        jdbcTemplate.jdbcTemplate.execute("DELETE FROM mv_product_rank_weekly")
        jdbcTemplate.jdbcTemplate.execute("DELETE FROM mv_product_rank_monthly")
    }

    private fun insertScoreConfig() {
        val sql = """
            INSERT INTO ranking_score_config (config_key, config_value, description, created_at, updated_at)
            VALUES (:key, :value, :desc, NOW(), NOW())
        """
        listOf(
            mapOf("key" to "VIEW_WEIGHT", "value" to 0.1, "desc" to "조회 가중치"),
            mapOf("key" to "LIKE_WEIGHT", "value" to 0.2, "desc" to "좋아요 가중치"),
            mapOf("key" to "ORDER_WEIGHT", "value" to 0.6, "desc" to "주문 가중치"),
        ).forEach { config ->
            jdbcTemplate.update(sql, MapSqlParameterSource(config))
        }
    }

    private fun insertEventLogs(date: LocalDate) {
        val sql = """
            INSERT INTO ranking_event_log (product_id, event_type, event_value, occurred_date, event_id, created_at)
            VALUES (:productId, :eventType, :eventValue, :date, :eventId, NOW())
        """
        val events = listOf(
            mapOf("productId" to 1L, "eventType" to "VIEW", "eventValue" to 0.8, "date" to date, "eventId" to "v-1-$date"),
            mapOf("productId" to 1L, "eventType" to "LIKE", "eventValue" to 1.0, "date" to date, "eventId" to "l-1-$date"),
            mapOf("productId" to 1L, "eventType" to "ORDER", "eventValue" to 50000.0, "date" to date, "eventId" to "o-1-$date"),
            mapOf("productId" to 2L, "eventType" to "VIEW", "eventValue" to 0.9, "date" to date, "eventId" to "v-2-$date"),
            mapOf("productId" to 2L, "eventType" to "LIKE", "eventValue" to 1.0, "date" to date, "eventId" to "l-2-$date"),
        )
        events.forEach { event ->
            jdbcTemplate.update(sql, MapSqlParameterSource(event))
        }
    }

    private fun queryMvWeekly(periodStartDate: LocalDate): List<Map<String, Any>> {
        val sql = "SELECT * FROM mv_product_rank_weekly WHERE period_start_date = :date ORDER BY `rank`"
        return jdbcTemplate.queryForList(sql, MapSqlParameterSource("date", periodStartDate))
    }

    private fun queryMvMonthly(periodStartDate: LocalDate): List<Map<String, Any>> {
        val sql = "SELECT * FROM mv_product_rank_monthly WHERE period_start_date = :date ORDER BY `rank`"
        return jdbcTemplate.queryForList(sql, MapSqlParameterSource("date", periodStartDate))
    }
}
