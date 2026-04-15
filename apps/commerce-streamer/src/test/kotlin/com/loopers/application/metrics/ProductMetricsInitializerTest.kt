package com.loopers.application.metrics

import com.loopers.domain.metrics.FakeProductMetricsDailyRepository
import com.loopers.domain.metrics.FakeProductMetricsRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate

class ProductMetricsInitializerTest {

    private lateinit var productMetricsRepository: FakeProductMetricsRepository
    private lateinit var productMetricsDailyRepository: FakeProductMetricsDailyRepository
    private lateinit var initializer: ProductMetricsInitializer

    @BeforeEach
    fun setUp() {
        productMetricsRepository = FakeProductMetricsRepository()
        productMetricsDailyRepository = FakeProductMetricsDailyRepository()
        initializer = ProductMetricsInitializer(productMetricsRepository, productMetricsDailyRepository)
    }

    @Nested
    @DisplayName("findOrCreate 시")
    inner class FindOrCreate {

        @Test
        @DisplayName("첫 insert에서 복구 가능한 DIVEx 발생 시 재조회 후 성공한다")
        fun `복구 가능한 DIVEx 발생 시 재조회 성공`() {
            productMetricsRepository.conflictsRemaining = 1
            productMetricsRepository.recoverableConflict = true

            val result = initializer.findOrCreate(1L)

            assertThat(result.productId).isEqualTo(1L)
        }

        @Test
        @DisplayName("복구 불가 DIVEx 발생 시 예외가 전파된다")
        fun `복구 불가 DIVEx는 예외가 전파된다`() {
            productMetricsRepository.conflictsRemaining = 1
            productMetricsRepository.recoverableConflict = false

            assertThatThrownBy { initializer.findOrCreate(1L) }
                .isInstanceOf(DataIntegrityViolationException::class.java)
        }

        @Test
        @DisplayName("이미 존재하는 경우 기존 레코드를 반환한다")
        fun `이미 존재하면 기존 레코드 반환`() {
            initializer.findOrCreate(1L)

            val result = initializer.findOrCreate(1L)

            assertThat(result.productId).isEqualTo(1L)
            assertThat(productMetricsRepository.findByProductId(1L)).isNotNull()
        }
    }

    @Nested
    @DisplayName("findOrCreateDaily 시")
    inner class FindOrCreateDaily {

        private val date = LocalDate.of(2026, 4, 15)

        @Test
        @DisplayName("첫 insert에서 복구 가능한 DIVEx 발생 시 재조회 후 성공한다")
        fun `복구 가능한 DIVEx 발생 시 재조회 성공`() {
            productMetricsDailyRepository.conflictsRemaining = 1
            productMetricsDailyRepository.recoverableConflict = true

            val result = initializer.findOrCreateDaily(date, 1L)

            assertThat(result.productId).isEqualTo(1L)
            assertThat(result.metricDate).isEqualTo(date)
        }

        @Test
        @DisplayName("복구 불가 DIVEx 발생 시 예외가 전파된다")
        fun `복구 불가 DIVEx는 예외가 전파된다`() {
            productMetricsDailyRepository.conflictsRemaining = 1
            productMetricsDailyRepository.recoverableConflict = false

            assertThatThrownBy { initializer.findOrCreateDaily(date, 1L) }
                .isInstanceOf(DataIntegrityViolationException::class.java)
        }

        @Test
        @DisplayName("이미 존재하는 경우 기존 레코드를 반환한다")
        fun `이미 존재하면 기존 레코드 반환`() {
            initializer.findOrCreateDaily(date, 1L)

            val result = initializer.findOrCreateDaily(date, 1L)

            assertThat(result.productId).isEqualTo(1L)
            assertThat(productMetricsDailyRepository.findByDateAndProductId(date, 1L)).isNotNull()
        }
    }
}
