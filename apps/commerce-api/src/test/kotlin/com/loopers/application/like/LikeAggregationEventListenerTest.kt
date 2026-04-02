package com.loopers.application.like

import com.loopers.application.event.LikeActionType
import com.loopers.application.event.LikeChangedEvent
import com.loopers.application.product.ProductCacheStore
import com.loopers.domain.product.ProductService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LikeAggregationEventListener")
class LikeAggregationEventListenerTest {
    private val productService: ProductService = mockk()
    private val productCacheStore: ProductCacheStore = mockk(relaxed = true)
    private val listener = LikeAggregationEventListener(productService, productCacheStore)

    @DisplayName("좋아요 이벤트를 받으면 상품 좋아요 수를 증가시키고 캐시를 무효화한다")
    @Test
    fun incrementsLikesCountAndEvictsCache_whenLikeEventReceived() {
        val event = LikeChangedEvent(userId = 1L, productId = 10L, actionType = LikeActionType.LIKE)
        every { productService.incrementLikesCount(10L) } just runs

        listener.handle(event)

        verify(exactly = 1) { productService.incrementLikesCount(10L) }
        verify(exactly = 1) { productCacheStore.evictProductDetail(10L) }
        verify(exactly = 1) { productCacheStore.evictProductList() }
    }

    @DisplayName("좋아요 취소 이벤트를 받으면 상품 좋아요 수를 감소시키고 캐시를 무효화한다")
    @Test
    fun decrementsLikesCountAndEvictsCache_whenUnlikeEventReceived() {
        val event = LikeChangedEvent(userId = 1L, productId = 10L, actionType = LikeActionType.UNLIKE)
        every { productService.decrementLikesCount(10L) } just runs

        listener.handle(event)

        verify(exactly = 1) { productService.decrementLikesCount(10L) }
        verify(exactly = 1) { productCacheStore.evictProductDetail(10L) }
        verify(exactly = 1) { productCacheStore.evictProductList() }
    }

    @DisplayName("집계 처리 중 예외가 발생해도 예외를 전파하지 않는다")
    @Test
    fun doesNotThrow_whenAggregationFails() {
        val event = LikeChangedEvent(userId = 1L, productId = 10L, actionType = LikeActionType.LIKE)
        every { productService.incrementLikesCount(10L) } throws IllegalStateException("aggregation failure")

        assertThatCode { listener.handle(event) }
            .doesNotThrowAnyException()
    }
}
