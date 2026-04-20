package com.loopers.batch.job.ranking.aggregate

import com.loopers.utils.DatabaseCleanUp
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = ["spring.batch.job.name=${WeeklyRankingAggregationJobConfig.JOB_NAME}"])
class WeeklyRankingAggregationJobTest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(WeeklyRankingAggregationJobConfig.JOB_NAME) private val job: Job,
    private val jdbcTemplate: JdbcTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private val periodKey = "2026-W16"
    private val startDate = "2026-04-13"
    private val endDate = "2026-04-19"

    @BeforeEach
    fun setUp() {
        // 브랜드 생성
        jdbcTemplate.update(
            "INSERT INTO brand (name, description, image_url, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', NOW(), NOW())",
            "TestBrand",
            "테스트 브랜드",
            "https://example.com/brand.jpg",
        )
        val brandId = jdbcTemplate.queryForObject("SELECT id FROM brand LIMIT 1", Long::class.java)!!

        // 상품 5개 생성
        for (i in 1..5) {
            jdbcTemplate.update(
                """
                INSERT INTO product (brand_id, name, description, price, stock_quantity, like_count, image_url, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, ?, 'ACTIVE', NOW(), NOW())
                """.trimIndent(),
                brandId,
                "Product_$i",
                "설명_$i",
                10000L * i,
                100,
                "https://example.com/product_$i.jpg",
            )
        }
        val productIds = jdbcTemplate.queryForList("SELECT id FROM product ORDER BY id", Long::class.java)

        // 각 상품에 서로 다른 지표 데이터 적재 (점수 차이가 명확하게)
        data class MetricSeed(val idx: Int, val view: Long, val like: Long, val order: Long, val amount: Long)
        val metrics = listOf(
            MetricSeed(0, 1000L, 500L, 100L, 5000000L),
            MetricSeed(1, 800L, 400L, 80L, 3000000L),
            MetricSeed(2, 600L, 300L, 60L, 2000000L),
            MetricSeed(3, 400L, 200L, 40L, 1000000L),
            MetricSeed(4, 200L, 100L, 20L, 500000L),
        )
        for (m in metrics) {
            jdbcTemplate.update(
                """
                INSERT INTO product_metrics_daily
                    (product_id, metric_date, view_count, like_count, order_count, order_amount_sum, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """.trimIndent(),
                productIds[m.idx],
                "2026-04-15",
                m.view,
                m.like,
                m.order,
                m.amount,
            )
        }
    }

    @AfterEach
    fun tearDown() {
        jdbcTemplate.update("DELETE FROM rank_staging")
        jdbcTemplate.update("DELETE FROM mv_product_rank_weekly")
        jdbcTemplate.update("DELETE FROM product_metrics_daily")
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("weeklyRankingAggregationJob 실행 시 mv_product_rank_weekly에 5개 row가 rank_value 1~5 순으로 적재된다")
    @Test
    fun shouldAggregateWeeklyRanking() {
        // arrange
        jobLauncherTestUtils.job = job
        val params = JobParametersBuilder()
            .addString("targetDate", "2026-04-13")
            .addString("period", RankingPeriod.WEEKLY.name)
            .addString("periodKey", periodKey)
            .addString("startDate", startDate)
            .addString("endDate", endDate)
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters()

        // act
        val jobExecution = jobLauncherTestUtils.launchJob(params)

        // assert
        val ranks = jdbcTemplate.queryForList(
            "SELECT rank_value FROM mv_product_rank_weekly WHERE period_key = ? ORDER BY rank_value",
            Int::class.java,
            periodKey,
        )
        val scores = jdbcTemplate.queryForList(
            "SELECT score FROM mv_product_rank_weekly WHERE period_key = ? ORDER BY rank_value",
            Double::class.java,
            periodKey,
        )
        val stagingCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM rank_staging",
            Int::class.java,
        )!!

        assertAll(
            { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(ranks).hasSize(5) },
            { assertThat(ranks).containsExactly(1, 2, 3, 4, 5) },
            { assertThat(scores).isSortedAccordingTo(Comparator.reverseOrder()) },
            { assertThat(stagingCount).isEqualTo(0) },
        )
    }
}
