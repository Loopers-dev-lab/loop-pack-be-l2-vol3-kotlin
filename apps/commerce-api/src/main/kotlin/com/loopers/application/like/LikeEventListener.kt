package com.loopers.application.like

import com.loopers.config.async.AsyncConfig
import com.loopers.domain.like.event.LikeEvent
import com.loopers.domain.product.ProductService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 좋아요 이벤트 리스너.
 *
 * likeCount 업데이트를 AFTER_COMMIT으로 분리하여
 * 집계 실패가 좋아요 자체에 영향을 주지 않도록 한다.
 */
@Component
class LikeEventListener(
    private val productService: ProductService,
) {

    companion object {
        private val log = LoggerFactory.getLogger(LikeEventListener::class.java)
    }

    @Async(AsyncConfig.EVENT_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleMetrics(event: LikeEvent.Liked) {
        try {
            productService.increaseLikeCount(event.aggregateId)
            log.info(
                "[좋아요 집계] likeCount 증가 [productId={}, userId={}]",
                event.aggregateId,
                event.userId,
            )
        } catch (e: Exception) {
            log.warn(
                "likeCount 증가 실패 — eventual consistency로 보정 예정 [productId={}]",
                event.aggregateId,
                e,
            )
        }
    }

    @Async(AsyncConfig.EVENT_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleMetrics(event: LikeEvent.Unliked) {
        try {
            productService.decreaseLikeCountIfExists(event.aggregateId)
            log.info(
                "[좋아요 집계] likeCount 감소 [productId={}, userId={}]",
                event.aggregateId,
                event.userId,
            )
        } catch (e: Exception) {
            log.warn(
                "likeCount 감소 실패 — eventual consistency로 보정 예정 [productId={}]",
                event.aggregateId,
                e,
            )
        }
    }
}
