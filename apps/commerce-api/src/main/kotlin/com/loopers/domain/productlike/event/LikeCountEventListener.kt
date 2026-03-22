package com.loopers.domain.productlike.event

import com.loopers.domain.productlike.ProductLikeCountRepository
import org.springframework.context.event.EventListener
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LikeCountEventListener(
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val cacheManager: CacheManager,
) {

    /**
     * 좋아요 카운트 projection 갱신 이벤트 처리.
     * 현재는 plain @EventListener 이므로 publisher 스레드에서 즉시 실행되며,
     * @Transactional 은 기본 전파(REQUIRED)로 기존 트랜잭션에 참여한다.
     */
    @EventListener(LikeCountEvent::class)
    @Transactional
    fun handleLikeCountEvent(event: LikeCountEvent) {
        when (event.type) {
            LikeCountEventType.INCREMENT -> productLikeCountRepository.increment(event.productId)
            LikeCountEventType.DECREMENT -> productLikeCountRepository.decrement(event.productId)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun evictProductInfoCacheAfterCommit(event: LikeCountEvent) {
        cacheManager.getCache("product-info")?.evict(event.productId)
    }
}
