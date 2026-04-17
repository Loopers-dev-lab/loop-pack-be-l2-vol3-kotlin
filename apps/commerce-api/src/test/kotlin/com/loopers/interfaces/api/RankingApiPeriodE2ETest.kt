package com.loopers.interfaces.api

import com.loopers.interfaces.api.ranking.RankingResponse
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import com.loopers.support.error.CommonErrorCode
import com.loopers.testcontainers.KafkaTestContainersConfig
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class, KafkaTestContainersConfig::class)
class RankingApiPeriodE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    private val jdbcTemplate: JdbcTemplate,
) {

    @BeforeEach
    fun setUp() {
        jdbcTemplate.execute(CREATE_WEEKLY_MV)
        jdbcTemplate.execute(CREATE_MONTHLY_MV)
        databaseCleanUp.truncateAllTables()
        jdbcTemplate.execute("TRUNCATE TABLE mv_product_rank_weekly")
        jdbcTemplate.execute("TRUNCATE TABLE mv_product_rank_monthly")
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE mv_product_rank_weekly")
        jdbcTemplate.execute("TRUNCATE TABLE mv_product_rank_monthly")
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @Test
    fun `period=weekly는 mv_product_rank_weekly에서 조회해 weekEnd 메타까지 포함해 반환한다`() {
        val weekEnd = LocalDate.of(2026, 4, 12)
        insertWeekly(productId = 100L, weekStart = weekEnd.minusDays(6), weekEnd = weekEnd, rank = 1, score = 80.0)
        insertWeekly(productId = 200L, weekStart = weekEnd.minusDays(6), weekEnd = weekEnd, rank = 2, score = 40.0)

        val response = testRestTemplate.exchange(
            "${ApiPaths.Rankings.BASE}?period=weekly",
            org.springframework.http.HttpMethod.GET,
            null,
            pageResponseType(),
        )

        val page = response.body?.data ?: error("응답 본문이 비어 있음")
        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(page.content).hasSize(2) },
            { assertThat(page.content.map { it.productId }).containsExactly(100L, 200L) },
            { assertThat(page.content[0].weekEnd).isEqualTo(weekEnd) },
            { assertThat(page.content[0].weekStart).isEqualTo(weekEnd.minusDays(6)) },
            { assertThat(page.content[0].yearMonth).isNull() },
        )
    }

    @Test
    fun `period=monthly는 mv_product_rank_monthly에서 조회해 yearMonth 메타까지 포함해 반환한다`() {
        insertMonthly(productId = 100L, yearMonth = "202604", rank = 1, score = 500.0)
        insertMonthly(productId = 200L, yearMonth = "202604", rank = 2, score = 300.0)

        val response = testRestTemplate.exchange(
            "${ApiPaths.Rankings.BASE}?period=monthly",
            org.springframework.http.HttpMethod.GET,
            null,
            pageResponseType(),
        )

        val page = response.body?.data ?: error("응답 본문이 비어 있음")
        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(page.content).hasSize(2) },
            { assertThat(page.content.map { it.yearMonth }).containsExactly("202604", "202604") },
            { assertThat(page.content[0].weekStart).isNull() },
            { assertThat(page.content[0].weekEnd).isNull() },
        )
    }

    @Test
    fun `period 파라미터가 없으면 daily로 동작해 200 OK로 응답한다 (하위 호환)`() {
        val response = testRestTemplate.exchange(
            ApiPaths.Rankings.BASE,
            org.springframework.http.HttpMethod.GET,
            null,
            pageResponseType(),
        )

        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.body?.meta?.errorCode).isNull() },
        )
    }

    @Test
    fun `MV가 비어 있으면 weekly 조회도 빈 응답으로 성공한다`() {
        val response = testRestTemplate.exchange(
            "${ApiPaths.Rankings.BASE}?period=weekly",
            org.springframework.http.HttpMethod.GET,
            null,
            pageResponseType(),
        )

        val page = response.body?.data ?: error("응답 본문이 비어 있음")
        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(page.content).isEmpty() },
            { assertThat(page.totalElements).isZero() },
        )
    }

    @Test
    fun `MV에 이전 버전과 최신 버전이 둘 다 있을 때 최신 week_end 버전만 응답한다`() {
        val olderWeekEnd = LocalDate.of(2026, 4, 5)
        val latestWeekEnd = LocalDate.of(2026, 4, 12)
        insertWeekly(productId = 999L, weekStart = olderWeekEnd.minusDays(6), weekEnd = olderWeekEnd, rank = 1, score = 999.9)
        insertWeekly(productId = 100L, weekStart = latestWeekEnd.minusDays(6), weekEnd = latestWeekEnd, rank = 1, score = 80.0)

        val response = testRestTemplate.exchange(
            "${ApiPaths.Rankings.BASE}?period=weekly",
            org.springframework.http.HttpMethod.GET,
            null,
            pageResponseType(),
        )

        val page = response.body?.data ?: error("응답 본문이 비어 있음")
        assertAll(
            { assertThat(page.content).hasSize(1) },
            { assertThat(page.content[0].productId).isEqualTo(100L) },
            { assertThat(page.content[0].weekEnd).isEqualTo(latestWeekEnd) },
        )
    }

    @Test
    fun `지원하지 않는 period 값은 400 INVALID_INPUT_VALUE를 반환한다`() {
        val response = testRestTemplate.exchange(
            "${ApiPaths.Rankings.BASE}?period=yearly",
            org.springframework.http.HttpMethod.GET,
            null,
            pageResponseType(),
        )

        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            { assertThat(response.body?.meta?.errorCode).isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE.code) },
        )
    }

    private fun insertWeekly(productId: Long, weekStart: LocalDate, weekEnd: LocalDate, rank: Int, score: Double) {
        jdbcTemplate.update(
            INSERT_WEEKLY_SQL,
            productId,
            java.sql.Date.valueOf(weekEnd),
            java.sql.Date.valueOf(weekStart),
            0L,
            0L,
            0L,
            score,
            rank,
            Timestamp.from(ZonedDateTime.now().toInstant()),
        )
    }

    private fun insertMonthly(productId: Long, yearMonth: String, rank: Int, score: Double) {
        jdbcTemplate.update(
            INSERT_MONTHLY_SQL,
            productId,
            yearMonth,
            0L,
            0L,
            0L,
            score,
            rank,
            Timestamp.from(ZonedDateTime.now().toInstant()),
        )
    }

    private fun pageResponseType(): ParameterizedTypeReference<ApiResponse<PageResult<RankingResponse>>> =
        object : ParameterizedTypeReference<ApiResponse<PageResult<RankingResponse>>>() {}

    companion object {
        private const val CREATE_WEEKLY_MV = """
            CREATE TABLE IF NOT EXISTS mv_product_rank_weekly (
                product_id BIGINT NOT NULL,
                week_end DATE NOT NULL,
                week_start DATE NOT NULL,
                view_count BIGINT NOT NULL DEFAULT 0,
                like_count BIGINT NOT NULL DEFAULT 0,
                order_count BIGINT NOT NULL DEFAULT 0,
                total_score DOUBLE NOT NULL DEFAULT 0,
                rank_position INT NOT NULL,
                updated_at DATETIME(6) NOT NULL,
                PRIMARY KEY (product_id, week_end)
            )
        """

        private const val CREATE_MONTHLY_MV = """
            CREATE TABLE IF NOT EXISTS mv_product_rank_monthly (
                product_id BIGINT NOT NULL,
                yearmonth VARCHAR(6) NOT NULL,
                view_count BIGINT NOT NULL DEFAULT 0,
                like_count BIGINT NOT NULL DEFAULT 0,
                order_count BIGINT NOT NULL DEFAULT 0,
                total_score DOUBLE NOT NULL DEFAULT 0,
                rank_position INT NOT NULL,
                updated_at DATETIME(6) NOT NULL,
                PRIMARY KEY (product_id, yearmonth)
            )
        """

        private const val INSERT_WEEKLY_SQL = """
            INSERT INTO mv_product_rank_weekly
              (product_id, week_end, week_start, view_count, like_count, order_count, total_score, rank_position, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val INSERT_MONTHLY_SQL = """
            INSERT INTO mv_product_rank_monthly
              (product_id, yearmonth, view_count, like_count, order_count, total_score, rank_position, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """
    }
}
