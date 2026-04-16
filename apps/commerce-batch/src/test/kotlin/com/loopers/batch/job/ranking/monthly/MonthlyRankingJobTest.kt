package com.loopers.batch.job.ranking.monthly

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
@TestPropertySource(properties = ["spring.batch.job.name=monthlyRankingJob"])
class MonthlyRankingJobTest @Autowired constructor(
    private val jobLauncher: JobLauncher,
    @Qualifier(MonthlyRankingJobConfig.JOB_NAME) private val job: Job,
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
    private fun queryMvMonthly(yearMonth: String): List<Array<Any>> {
        return entityManager.createNativeQuery(
            "SELECT product_id, score, rank_num, view_count, like_count, sales_count FROM mv_product_rank_monthly WHERE `year_month` = :yearMonth ORDER BY rank_num",
        )
            .setParameter("yearMonth", yearMonth)
            .resultList as List<Array<Any>>
    }

    @Test
    @DisplayName("일별 메트릭을 월 단위로 집계하여 MV에 적재한다")
    fun aggregatesMonthlyAndWritesToMv() {
        // arrange — 2026년 4월
        insertDailyMetrics(100L, LocalDate.of(2026, 4, 1), 100, 20, 10)
        insertDailyMetrics(100L, LocalDate.of(2026, 4, 15), 50, 10, 5)
        insertDailyMetrics(200L, LocalDate.of(2026, 4, 10), 200, 50, 20)

        // 범위 밖 (3월)
        insertDailyMetrics(100L, LocalDate.of(2026, 3, 31), 999, 999, 999)

        // act
        val params = JobParametersBuilder()
            .addString("targetDate", "20260415")
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobLauncher.run(job, params)

        // assert
        assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)

        val results = transactionTemplate.execute { queryMvMonthly("2026-04") }!!
        assertThat(results).hasSize(2)

        // 상품 200: 200×0.1 + 50×0.2 + 20×0.7 = 44.0 → rank 1
        val rank1 = results[0]
        assertThat((rank1[0] as Number).toLong()).isEqualTo(200L)
        assertThat((rank1[1] as Number).toDouble()).isCloseTo(44.0, Offset.offset(0.01))

        // 상품 100: (100+50)×0.1 + (20+10)×0.2 + (10+5)×0.7 = 31.5 → rank 2
        val rank2 = results[1]
        assertThat((rank2[0] as Number).toLong()).isEqualTo(100L)
        assertThat((rank2[1] as Number).toDouble()).isCloseTo(31.5, Offset.offset(0.01))
    }

    @Test
    @DisplayName("같은 targetDate로 재실행해도 동일한 결과가 유지된다 (멱등성)")
    fun idempotentReExecution() {
        // arrange
        insertDailyMetrics(100L, LocalDate.of(2026, 4, 1), 100, 20, 10)

        // act
        jobLauncher.run(
            job,
            JobParametersBuilder().addString("targetDate", "20260415").addLong("run.id", 1L).toJobParameters(),
        )
        val execution2 = jobLauncher.run(
            job,
            JobParametersBuilder().addString("targetDate", "20260415").addLong("run.id", 2L).toJobParameters(),
        )

        // assert
        assertThat(execution2.status).isEqualTo(BatchStatus.COMPLETED)
        val results = transactionTemplate.execute { queryMvMonthly("2026-04") }!!
        assertThat(results).hasSize(1)
    }
}
