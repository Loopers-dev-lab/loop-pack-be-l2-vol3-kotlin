package com.loopers.application.like

import com.loopers.domain.catalog.ProductCache
import com.loopers.domain.common.event.ProductLikedEvent
import com.loopers.domain.common.event.ProductUnlikedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductLikeEventListener(
    private val productCache: ProductCache,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("eventTaskExecutor")
    @TransactionalEventListener
    fun handleProductLiked(event: ProductLikedEvent) {
        if (!event.isNewLike) return
        try {
            productCache.evictProduct(event.productId)
        } catch (e: Exception) {
            log.error("좋아요 캐시 무효화 실패 - productId: {}", event.productId, e)
        }
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener
    fun handleProductUnliked(event: ProductUnlikedEvent) {
        try {
            productCache.evictProduct(event.productId)
        } catch (e: Exception) {
            log.error("좋아요 취소 캐시 무효화 실패 - productId: {}", event.productId, e)
        }
    }
}
