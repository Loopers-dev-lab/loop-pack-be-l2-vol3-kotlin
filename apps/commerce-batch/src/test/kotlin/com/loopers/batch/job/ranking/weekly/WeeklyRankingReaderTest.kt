package com.loopers.batch.job.ranking.weekly

import com.loopers.batch.job.ranking.ProductAggregateDto

import com.loopers.utils.DatabaseCleanUp
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import javax.sql.DataSource

@SpringBatchTest
@SpringBootTest
@TestPropertySource(properties = ["spring.batch.job.enabled=false"])
class WeeklyRankingReaderTest @Autowired constructor(
    @PersistenceContext private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
    private val dataSource: DataSource,
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

    @DisplayName("주간 랭킹 Reader 시,")
    @Nested
    inner class WeeklyReader {

        @DisplayName("targetDate 기준 해당 주차 범위의 데이터를 상품별로 집계한다.")
        @Test
        fun aggregatesByProductForWeekRange() {
            // arrange — 2026-04-13(월) ~ 2026-04-19(일) = 2026-W16
            val targetDate = LocalDate.of(2026, 4, 15) // 수요일
            insertDailyMetrics(100L, LocalDate.of(2026, 4, 13), 10, 2, 1) // 월
            insertDailyMetrics(100L, LocalDate.of(2026, 4, 14), 20, 3, 2) // 화
            insertDailyMetrics(200L, LocalDate.of(2026, 4, 13), 5, 1, 0) // 다른 상품

            // 범위 밖 데이터 (이전 주)
            insertDailyMetrics(100L, LocalDate.of(2026, 4, 12), 999, 999, 999) // 일요일 = W15

            // act
            val reader = weeklyRankingReader(dataSource, targetDate, 100)
            reader.afterPropertiesSet()
            reader.open(ExecutionContext())

            val results = mutableListOf<ProductAggregateDto>()
            var item = reader.read()
            while (item != null) {
                results.add(item)
                item = reader.read()
            }
            reader.close()

            // assert
            assertThat(results).hasSize(2)

            val product100 = results.find { it.productId == 100L }!!
            assertThat(product100.viewCount).isEqualTo(30L) // 10 + 20
            assertThat(product100.likeCount).isEqualTo(5L) // 2 + 3
            assertThat(product100.salesCount).isEqualTo(3L) // 1 + 2

            val product200 = results.find { it.productId == 200L }!!
            assertThat(product200.viewCount).isEqualTo(5L)
        }
    }
}
