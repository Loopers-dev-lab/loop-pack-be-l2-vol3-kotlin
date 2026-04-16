package com.loopers.domain.event.handler

import com.loopers.domain.event.ProductViewedEvent
import com.loopers.infrastructure.productmetrics.ProductMetricsRepository
import com.loopers.infrastructure.productmetrics.ProductMetricsDailyRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("ProductViewedEventHandler 테스트")
class ProductViewedEventHandlerTest {

    private lateinit var productMetricsRepository: ProductMetricsRepository
    private lateinit var productMetricsDailyRepository: ProductMetricsDailyRepository
    private lateinit var handler: ProductViewedEventHandler

    @BeforeEach
    fun setUp() {
        productMetricsRepository = mockk()
        productMetricsDailyRepository = mockk()
        handler = ProductViewedEventHandler(productMetricsRepository, productMetricsDailyRepository)
    }

    @Test
    @DisplayName("ProductViewedEvent를 받으면 viewCount를 증가시킨다")
    fun shouldIncrementViewCount() {
        // Given
        val event = ProductViewedEvent(
            productId = 1L,
            userId = 100L,
        )

        every { productMetricsRepository.incrementViewCount(1L) } returns Unit
        every { productMetricsDailyRepository.incrementViewCount(any(), any()) } returns Unit

        // When
        handler.handle(event)

        // Then
        verify { productMetricsRepository.incrementViewCount(1L) }
    }

    @Test
    @DisplayName("서로 다른 productId는 각각 증가시킨다")
    fun shouldIncrementDifferentProductIds() {
        // Given
        val event1 = ProductViewedEvent(productId = 1L, userId = 100L)
        val event2 = ProductViewedEvent(productId = 2L, userId = 100L)

        every { productMetricsRepository.incrementViewCount(any()) } returns Unit
        every { productMetricsDailyRepository.incrementViewCount(any(), any()) } returns Unit

        // When
        handler.handle(event1)
        handler.handle(event2)

        // Then
        verify { productMetricsRepository.incrementViewCount(1L) }
        verify { productMetricsRepository.incrementViewCount(2L) }
    }
}
