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

        @DisplayName("기존 메트릭이 없으면, 새로 생성하고 likeCount=1이 된다.")
        @Test
        fun createsNewMetrics_whenNotExists() {
            // arrange
            whenever(productMetricsRepository.findByProductId(1L)).thenReturn(null)
            whenever(productMetricsRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            metricsAggregationService.incrementLikeCount(1L, 100L)

            // assert (save 2회: 생성 1회 + 업데이트 1회)
            verify(productMetricsRepository, org.mockito.kotlin.times(2)).save(any())
        }

        @DisplayName("stale 이벤트이면, 무시한다.")
        @Test
        fun ignoresStaleEvent() {
            // arrange
            val metrics = ProductMetrics(productId = 1L)
            org.springframework.test.util.ReflectionTestUtils.setField(metrics, "version", 200L)
            whenever(productMetricsRepository.findByProductId(1L)).thenReturn(metrics)

            // act
            metricsAggregationService.incrementLikeCount(1L, 100L)

            // assert
            verify(productMetricsRepository, never()).save(any())
        }
    }

    @DisplayName("viewCount를 증가시킬 때,")
    @Nested
    inner class IncrementViewCount {

        @DisplayName("정상 이벤트이면, viewCount가 1 증가한다.")
        @Test
        fun incrementsViewCount() {
            // arrange
            whenever(productMetricsRepository.findByProductId(1L)).thenReturn(null)
            whenever(productMetricsRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            metricsAggregationService.incrementViewCount(1L, 100L)

            // assert (save 2회: 생성 1회 + 업데이트 1회)
            verify(productMetricsRepository, org.mockito.kotlin.times(2)).save(any())
        }
    }
}
