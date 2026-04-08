package com.loopers.application.event

import com.loopers.domain.event.ProductLikedEvent
import com.loopers.domain.event.ProductUnlikedEvent
import com.loopers.domain.product.ProductService
import com.loopers.infrastructure.cache.ProductCacheRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class LikeEventListenerTest {
    private val productService: ProductService = mock()
    private val productCacheRepository: ProductCacheRepository = mock()
    private val listener = LikeEventListener(productService, productCacheRepository)

    @DisplayName("ProductLikedEvent를 받으면, likeCount를 증가시키고 캐시를 무효화한다.")
    @Test
    fun increasesLikeCountAndEvictsCache_whenProductLiked() {
        // arrange
        val event = ProductLikedEvent(userId = 1L, productId = 100L)

        // act
        listener.handleProductLiked(event)

        // assert
        verify(productService).increaseLikeCount(100L)
        verify(productCacheRepository).evict(100L)
    }

    @DisplayName("ProductUnlikedEvent를 받으면, likeCount를 감소시키고 캐시를 무효화한다.")
    @Test
    fun decreasesLikeCountAndEvictsCache_whenProductUnliked() {
        // arrange
        val event = ProductUnlikedEvent(userId = 1L, productId = 100L)

        // act
        listener.handleProductUnliked(event)

        // assert
        verify(productService).decreaseLikeCount(100L)
        verify(productCacheRepository).evict(100L)
    }
}
