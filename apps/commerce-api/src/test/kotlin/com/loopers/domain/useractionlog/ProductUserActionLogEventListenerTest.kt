package com.loopers.domain.useractionlog

import com.loopers.domain.product.event.ProductViewedEvent
import com.loopers.domain.productlike.event.ProductLikedEvent
import com.loopers.domain.productlike.event.ProductUnlikedEvent
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

/**
 * ProductUserActionLogEventListener의 이벤트 처리 로직을 검증
 * (실제 DB 트랜잭션 없이 리스너 호출만 테스트)
 */
class ProductUserActionLogEventListenerTest {

    private val mockPersistenceService: UserActionLogPersistenceService = mockk(relaxed = true)
    private val listener = ProductUserActionLogEventListener(mockPersistenceService)

    @Test
    fun `ProductViewedEvent 발행 시 appendSafely를 호출한다`() {
        // Given
        val event = ProductViewedEvent(
            productId = 123L,
            userId = 456L,
            dedupeKey = "product.viewed:456:123:abc",
        )

        // When
        listener.onProductViewed(event)

        // Then
        verify(exactly = 1) { mockPersistenceService.appendIfAbsent(any()) }
    }

    @Test
    fun `ProductLikedEvent 발행 시 appendSafely를 호출한다`() {
        // Given
        val event = ProductLikedEvent(
            productId = 789L,
            userId = 456L,
            dedupeKey = "product.liked:456:789:def",
        )

        // When
        listener.onProductLiked(event)

        // Then
        verify(exactly = 1) { mockPersistenceService.appendIfAbsent(any()) }
    }

    @Test
    fun `ProductUnlikedEvent 발행 시 appendSafely를 호출한다`() {
        // Given
        val event = ProductUnlikedEvent(
            productId = 999L,
            userId = 456L,
            dedupeKey = "product.unliked:456:999:ghi",
        )

        // When
        listener.onProductUnliked(event)

        // Then
        verify(exactly = 1) { mockPersistenceService.appendIfAbsent(any()) }
    }
}
