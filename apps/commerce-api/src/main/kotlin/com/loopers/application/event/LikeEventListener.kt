package com.loopers.application.event

import com.loopers.domain.event.ProductLikedEvent
import com.loopers.domain.event.ProductUnlikedEvent
import com.loopers.domain.product.ProductService
import com.loopers.infrastructure.cache.ProductCacheRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LikeEventListener(
    private val productService: ProductService,
    private val productCacheRepository: ProductCacheRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleProductLiked(event: ProductLikedEvent) {
        log.info("[UserAction] PRODUCT_LIKED userId={} productId={}", event.userId, event.productId)
        productService.increaseLikeCount(event.productId)
        productCacheRepository.evict(event.productId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleProductUnliked(event: ProductUnlikedEvent) {
        log.info("[UserAction] PRODUCT_UNLIKED userId={} productId={}", event.userId, event.productId)
        productService.decreaseLikeCount(event.productId)
        productCacheRepository.evict(event.productId)
    }
}
