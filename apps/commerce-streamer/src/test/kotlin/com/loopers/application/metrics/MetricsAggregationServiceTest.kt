package com.loopers.application.metrics

import com.loopers.domain.metrics.ProductMetrics
import com.loopers.domain.metrics.ProductMetricsRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class MetricsAggregationServiceTest {

    @Mock
    private lateinit var productMetricsRepository: ProductMetricsRepository

    @InjectMocks
    private lateinit var metricsAggregationService: MetricsAggregationService

    @DisplayName("likeCount를 증가시킬 때,")
    @Nested
    inner class IncrementLikeCount {

        @DisplayName("기존 메트릭이 있으면, 원자적 UPDATE로 likeCount를 증가시킨다.")
        @Test
        fun incrementsLikeCount_whenMetricsExists() {
            // arrange
            val metrics = ProductMetrics(productId = 1L)
            whenever(productMetricsRepository.findByProductId(1L)).thenReturn(metrics)
            whenever(productMetricsRepository.incrementLikeCount(1L, 100L)).thenReturn(1)

            // act
            metricsAggregationService.incrementLikeCount(1L, 100L)

            // assert
            verify(productMetricsRepository).incrementLikeCount(1L, 100L)
            verify(productMetricsRepository, never()).save(any())
        }

        @DisplayName("기존 메트릭이 없으면, 새로 생성한 뒤 원자적 UPDATE를 실행한다.")
        @Test
        fun createsNewMetrics_whenNotExists() {
            // arrange
            whenever(productMetricsRepository.findByProductId(1L)).thenReturn(null)
            whenever(productMetricsRepository.save(any())).thenAnswer { it.arguments[0] }
            whenever(productMetricsRepository.incrementLikeCount(1L, 100L)).thenReturn(1)

            // act
            metricsAggregationService.incrementLikeCount(1L, 100L)

            // assert
            verify(productMetricsRepository).save(any())
            verify(productMetricsRepository).incrementLikeCount(1L, 100L)
        }

        @DisplayName("stale 이벤트이면, 원자적 UPDATE의 WHERE 조건으로 무시된다 (갱신 0건).")
        @Test
        fun ignoresStaleEvent() {
            // arrange
            val metrics = ProductMetrics(productId = 1L)
            whenever(productMetricsRepository.findByProductId(1L)).thenReturn(metrics)
            whenever(productMetricsRepository.incrementLikeCount(1L, 100L)).thenReturn(0)

            // act
            metricsAggregationService.incrementLikeCount(1L, 100L)

            // assert — UPDATE가 실행되지만 WHERE 조건에 걸려 0건 갱신
            verify(productMetricsRepository).incrementLikeCount(1L, 100L)
        }
    }

    @DisplayName("viewCount를 증가시킬 때,")
    @Nested
    inner class IncrementViewCount {

        @DisplayName("정상 이벤트이면, 원자적 UPDATE로 viewCount를 증가시킨다.")
        @Test
        fun incrementsViewCount() {
            // arrange
            val metrics = ProductMetrics(productId = 1L)
            whenever(productMetricsRepository.findByProductId(1L)).thenReturn(metrics)
            whenever(productMetricsRepository.incrementViewCount(1L, 100L)).thenReturn(1)

            // act
            metricsAggregationService.incrementViewCount(1L, 100L)

            // assert
            verify(productMetricsRepository).incrementViewCount(1L, 100L)
        }
    }
}
