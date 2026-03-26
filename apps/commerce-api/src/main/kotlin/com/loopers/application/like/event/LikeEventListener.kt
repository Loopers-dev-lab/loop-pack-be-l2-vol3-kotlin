package com.loopers.application.like.event

import com.loopers.application.product.ProductCacheStore
import com.loopers.domain.event.LikeCancelledEvent
import com.loopers.domain.event.LikeCreatedEvent
import com.loopers.domain.product.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate

@Component
class LikeEventListener(
    private val productRepository: ProductRepository,
    private val productCacheStore: ProductCacheStore,
    private val transactionTemplate: TransactionTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleLikeCreated(event: LikeCreatedEvent) {
        try {
            transactionTemplate.executeWithoutResult {
                productRepository.increaseLikeCount(event.productId)
            }
            productCacheStore.evictDetail(event.productId)
            log.info("좋아요 이벤트 처리 완료 [userId={}, productId={}]", event.userId, event.productId)
        } catch (e: Exception) {
            log.error("좋아요 이벤트 처리 실패 [userId={}, productId={}]", event.userId, event.productId, e)
        }
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleLikeCancelled(event: LikeCancelledEvent) {
        try {
            transactionTemplate.executeWithoutResult {
                productRepository.decreaseLikeCount(event.productId)
            }
            productCacheStore.evictDetail(event.productId)
            log.info("좋아요 취소 이벤트 처리 완료 [userId={}, productId={}]", event.userId, event.productId)
        } catch (e: Exception) {
            log.error("좋아요 취소 이벤트 처리 실패 [userId={}, productId={}]", event.userId, event.productId, e)
        }
    }
}
