package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.utils.DatabaseCleanUp
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@Transactional
class ProductMetricsDailyUpsertIntegrationTest @Autowired constructor(
    private val productMetricsRepository: ProductMetricsRepository,
    private val productMetricsDailyJpaRepository: ProductMetricsDailyJpaRepository,
    private val entityManager: EntityManager,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("일별 viewCount upsert 시,")
    @Nested
    inner class IncrementDailyViewCount {

        @DisplayName("해당 날짜의 row가 없으면 새로 생성하고 viewCount가 1이 된다.")
        @Test
        fun createsNewRowWhenNotExists() {
            // arrange
            val productId = 100L
            val metricDate = LocalDate.of(2026, 4, 15)

            // act
            productMetricsRepository.incrementDailyViewCount(productId, metricDate)
            entityManager.flush()
            entityManager.clear()

            // assert
            val result = productMetricsDailyJpaRepository.findByProductIdAndMetricDate(productId, metricDate)
            assertThat(result).isNotNull
            assertThat(result!!.viewCount).isEqualTo(1L)
            assertThat(result.likeCount).isZero()
            assertThat(result.salesCount).isZero()
        }

        @DisplayName("같은 날짜에 두 번 호출하면 viewCount가 2가 된다.")
        @Test
        fun incrementsExistingRow() {
            // arrange
            val productId = 100L
            val metricDate = LocalDate.of(2026, 4, 15)

            // act
            productMetricsRepository.incrementDailyViewCount(productId, metricDate)
            productMetricsRepository.incrementDailyViewCount(productId, metricDate)
            entityManager.flush()
            entityManager.clear()

            // assert
            val result = productMetricsDailyJpaRepository.findByProductIdAndMetricDate(productId, metricDate)
            assertThat(result).isNotNull
            assertThat(result!!.viewCount).isEqualTo(2L)
        }

        @DisplayName("다른 날짜에 호출하면 별도 row가 생성된다.")
        @Test
        fun createsSeparateRowForDifferentDate() {
            // arrange
            val productId = 100L
            val date1 = LocalDate.of(2026, 4, 15)
            val date2 = LocalDate.of(2026, 4, 16)

            // act
            productMetricsRepository.incrementDailyViewCount(productId, date1)
            productMetricsRepository.incrementDailyViewCount(productId, date2)
            entityManager.flush()
            entityManager.clear()

            // assert
            val all = productMetricsDailyJpaRepository.findAll()
            assertThat(all).hasSize(2)
        }
    }
}
