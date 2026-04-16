package com.loopers.domain.event.handler

import com.loopers.domain.productlike.ProductLikeCountRepository
import com.loopers.domain.event.LikeCountEvent
import com.loopers.domain.event.LikeCountEventType
import com.loopers.infrastructure.productmetrics.ProductMetricsDailyRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("LikeCountEventHandler 테스트")
class LikeCountEventHandlerTest {

    private lateinit var productLikeCountRepository: ProductLikeCountRepository
    private lateinit var productMetricsDailyRepository: ProductMetricsDailyRepository
    private lateinit var handler: LikeCountEventHandler

    @BeforeEach
    fun setUp() {
        productLikeCountRepository = mockk()
        productMetricsDailyRepository = mockk()
        handler = LikeCountEventHandler(productLikeCountRepository, productMetricsDailyRepository)
    }

    @Test
    @DisplayName("INCREMENT 이벤트를 받으면 like_count를 증가시킨다")
    fun shouldIncrementLikeCount() {
        // Given
        val event = LikeCountEvent(
            productId = 1L,
            type = LikeCountEventType.INCREMENT,
            userId = 100L,
        )

        every { productLikeCountRepository.increment(1L) } returns Unit
        every { productMetricsDailyRepository.incrementLikeCount(any(), any(), any()) } returns Unit

        // When
        handler.handle(event)

        // Then
        verify { productLikeCountRepository.increment(1L) }
    }

    @Test
    @DisplayName("DECREMENT 이벤트를 받으면 like_count를 감소시킨다")
    fun shouldDecrementLikeCount() {
        // Given
        val event = LikeCountEvent(
            productId = 2L,
            type = LikeCountEventType.DECREMENT,
            userId = 100L,
        )

        every { productLikeCountRepository.decrement(2L) } returns Unit
        every { productMetricsDailyRepository.incrementLikeCount(any(), any(), any()) } returns Unit

        // When
        handler.handle(event)

        // Then
        verify { productLikeCountRepository.decrement(2L) }
    }
}
