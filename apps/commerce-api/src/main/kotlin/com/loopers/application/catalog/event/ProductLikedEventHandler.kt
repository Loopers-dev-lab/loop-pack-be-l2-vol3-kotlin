package com.loopers.application.catalog.event

import com.loopers.application.like.event.ProductLikedEvent
import com.loopers.application.like.event.ProductUnlikedEvent
import com.loopers.domain.catalog.product.ProductService
import com.loopers.infrastructure.catalog.product.ProductCacheService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductLikedEventHandler(
    private val productService: ProductService,
    private val productCacheService: ProductCacheService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    fun handleProductLiked(event: ProductLikedEvent) {
        log.info("[Event] ProductLiked: userId=${event.userId}, productId=${event.productId}")

        try {
            productService.incrementLikeCount(event.productId)
        } catch (ex: Exception) {
            log.error("[Event] likeCount 증가 실패: productId=${event.productId}, error=${ex.message}", ex)
        }

        try {
            productCacheService.evictProductDetail(event.productId)
            productCacheService.evictAllProductLists()
        } catch (ex: Exception) {
            log.error("[Event] 캐시 무효화 실패: productId=${event.productId}, error=${ex.message}", ex)
        }

        log.info("[UserAction] PRODUCT_LIKED: userId=${event.userId}, productId=${event.productId}")
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    fun handleProductUnliked(event: ProductUnlikedEvent) {
        log.info("[Event] ProductUnliked: userId=${event.userId}, productId=${event.productId}")

        try {
            productService.decrementLikeCount(event.productId)
        } catch (ex: Exception) {
            log.error("[Event] likeCount 감소 실패: productId=${event.productId}, error=${ex.message}", ex)
        }

        try {
            productCacheService.evictProductDetail(event.productId)
            productCacheService.evictAllProductLists()
        } catch (ex: Exception) {
            log.error("[Event] 캐시 무효화 실패: productId=${event.productId}, error=${ex.message}", ex)
        }

        log.info("[UserAction] PRODUCT_UNLIKED: userId=${event.userId}, productId=${event.productId}")
    }
}
