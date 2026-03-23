package com.loopers.application.product

import com.loopers.domain.like.event.ProductLikedEvent
import com.loopers.domain.like.event.ProductUnlikedEvent
import com.loopers.domain.product.ProductService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 상품 관련 이벤트 핸들러.
 *
 * 좋아요 집계(likeCount)는 부가 효과이므로 Eventual Consistency로 처리한다.
 * - AFTER_COMMIT: Like 트랜잭션 커밋 확인 후 실행
 * - @Async: 별도 스레드에서 실행하여 커넥션 풀 고갈 방지
 * - @Transactional(REQUIRES_NEW): 집계 UPDATE 쿼리를 위한 별도 트랜잭션
 */
@Component
class ProductEventHandler(
    private val productService: ProductService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onProductLiked(event: ProductLikedEvent) {
        try {
            productService.incrementLikeCount(event.productId)
        } catch (e: Exception) {
            log.error("좋아요 카운트 증가 실패: productId={}", event.productId, e)
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onProductUnliked(event: ProductUnlikedEvent) {
        try {
            productService.decrementLikeCount(event.productId)
        } catch (e: Exception) {
            log.error("좋아요 카운트 감소 실패: productId={}", event.productId, e)
        }
    }
}
