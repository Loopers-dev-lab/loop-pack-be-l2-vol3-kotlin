package com.loopers.batch.job.ranking.weekly

import com.loopers.utils.DatabaseCleanUp
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

@SpringBootTest
@TestPropertySource(properties = ["spring.batch.job.name=weeklyRankingJob"])
class WeeklyRankingJobTest @Autowired constructor(
    private val jobLauncher: JobLauncher,
    @Qualifier(WeeklyRankingJobConfig.JOB_NAME) private val job: Job,
    @PersistenceContext private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun insertDailyMetrics(productId: Long, metricDate: LocalDate, viewCount: Long, likeCount: Long, salesCount: Long) {
        transactionTemplate.executeWithoutResult {
            entityManager.createNativeQuery(
                "INSERT INTO product_metrics_daily (metric_date, product_id, view_count, like_count, sales_count) " +
                    "VALUES (:metricDate, :productId, :viewCount, :likeCount, :salesCount)",
            )
                .setParameter("metricDate", metricDate)
                .setParameter("productId", productId)
                .setParameter("viewCount", viewCount)
                .setParameter("likeCount", likeCount)
                .setParameter("salesCount", salesCount)
                .executeUpdate()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun queryMvWeekly(yearWeek: String): List<Array<Any>> {
        return entityManager.createNativeQuery(
            "SELECT product_id, score, rank_num, view_count, like_count, sales_count FROM mv_product_rank_weekly WHERE year_week = :yearWeek ORDER BY rank_num",
        )
            .setParameter("yearWeek", yearWeek)
            .resultList as List<Array<Any>>
    }

    @Test
    @DisplayName("일별 메트릭을 집계하여 MV에 점수 순 랭킹으로 적재한다")
    fun aggregatesAndWritesToMv() {
        // arrange — 2026-W16: 4/13(월) ~ 4/19(일)
        insertDailyMetrics(100L, LocalDate.of(2026, 4, 13), 100, 20, 10)
        insertDailyMetrics(100L, LocalDate.of(2026, 4, 14), 50, 10, 5)
        insertDailyMetrics(200L, LocalDate.of(2026, 4, 13), 200, 50, 20)

        // 범위 밖 데이터 (이전 주)
        insertDailyMetrics(100L, LocalDate.of(2026, 4, 12), 999, 999, 999)

        // act
        val params = JobParametersBuilder()
            .addString("targetDate", "20260415")
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobLauncher.run(job, params)

        // assert
        assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)

        val results = transactionTemplate.execute { queryMvWeekly("2026-W16") }!!
        assertThat(results).hasSize(2)

        // 상품 200: 200×0.1 + 50×0.2 + 20×0.7 = 20 + 10 + 14 = 44.0 → rank 1
        val rank1 = results[0]
        assertThat((rank1[0] as Number).toLong()).isEqualTo(200L)
        assertThat((rank1[1] as Number).toDouble()).isCloseTo(44.0, Offset.offset(0.01))
        assertThat((rank1[2] as Number).toInt()).isEqualTo(1)

        // 상품 100: (100+50)×0.1 + (20+10)×0.2 + (10+5)×0.7 = 15 + 6 + 10.5 = 31.5 → rank 2
        val rank2 = results[1]
        assertThat((rank2[0] as Number).toLong()).isEqualTo(100L)
        assertThat((rank2[1] as Number).toDouble()).isCloseTo(31.5, Offset.offset(0.01))
        assertThat((rank2[2] as Number).toInt()).isEqualTo(2)
    }

    @Test
    @DisplayName("동일 score를 가진 상품은 product_id 오름차순으로 결정적으로 rank가 부여된다")
    fun deterministicTieBreakByProductIdAsc() {
        // arrange — 세 상품 모두 동일한 집계 수치로 score가 같도록 구성 (2026-W16)
        insertDailyMetrics(300L, LocalDate.of(2026, 4, 13), 10, 10, 10)
        insertDailyMetrics(100L, LocalDate.of(2026, 4, 13), 10, 10, 10)
        insertDailyMetrics(200L, LocalDate.of(2026, 4, 13), 10, 10, 10)

        // act
        val params = JobParametersBuilder()
            .addString("targetDate", "20260415")
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobLauncher.run(job, params)

        // assert
        assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)

        val results = transactionTemplate.execute { queryMvWeekly("2026-W16") }!!
        assertThat(results).hasSize(3)

        // tie-break 테스트의 전제: 세 상품의 score가 실제로 동일해야 rank 순서가 product_id 기준으로만 결정됨을 증명 가능
        val scores = results.map { (it[1] as Number).toDouble() }
        assertThat(scores).allMatch { it == scores[0] }

        // score 동점일 때 product_id ASC 순으로 rank 1, 2, 3 부여되어야 한다
        assertThat((results[0][0] as Number).toLong()).isEqualTo(100L)
        assertThat((results[0][2] as Number).toInt()).isEqualTo(1)
        assertThat((results[1][0] as Number).toLong()).isEqualTo(200L)
        assertThat((results[1][2] as Number).toInt()).isEqualTo(2)
        assertThat((results[2][0] as Number).toLong()).isEqualTo(300L)
        assertThat((results[2][2] as Number).toInt()).isEqualTo(3)
    }

    @Test
    @DisplayName("같은 targetDate로 재실행해도 동일한 결과가 유지된다 (멱등성)")
    fun idempotentReExecution() {
        // arrange
        insertDailyMetrics(100L, LocalDate.of(2026, 4, 13), 100, 20, 10)

        // act — 2번 실행
        val params1 = JobParametersBuilder()
            .addString("targetDate", "20260415")
            .addLong("run.id", 1L)
            .toJobParameters()
        jobLauncher.run(job, params1)

        val params2 = JobParametersBuilder()
            .addString("targetDate", "20260415")
            .addLong("run.id", 2L)
            .toJobParameters()
        val execution2 = jobLauncher.run(job, params2)

        // assert
        assertThat(execution2.status).isEqualTo(BatchStatus.COMPLETED)
        val results = transactionTemplate.execute { queryMvWeekly("2026-W16") }!!
        assertThat(results).hasSize(1)
    }
}
