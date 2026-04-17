package com.loopers.application.like.event

import com.loopers.application.product.ProductCacheStore
import com.loopers.domain.event.LikeCancelledEvent
import com.loopers.domain.event.LikeCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LikeEventListener(
    private val productCacheStore: ProductCacheStore,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleLikeCreated(event: LikeCreatedEvent) {
        try {
            productCacheStore.evictDetail(event.productId)
        } catch (e: Exception) {
            log.error("좋아요 캐시 evict 실패 [productId={}]", event.productId, e)
        }
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleLikeCancelled(event: LikeCancelledEvent) {
        try {
            productCacheStore.evictDetail(event.productId)
        } catch (e: Exception) {
            log.error("좋아요 취소 캐시 evict 실패 [productId={}]", event.productId, e)
        }
    }
}
