package com.loopers.domain.productlike.event

import com.loopers.domain.productlike.ProductLikeCountRepository
import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.slf4j.LoggerFactory

@Component
class LikeCountEventListener(
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val cacheManager: CacheManager,
) {
    companion object {
        private val log = LoggerFactory.getLogger(LikeCountEventListener::class.java)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventListenerExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleLikeCountEvent(event: LikeCountEvent) {
        try {
            when (event.type) {
                LikeCountEventType.INCREMENT -> productLikeCountRepository.increment(event.productId)
                LikeCountEventType.DECREMENT -> productLikeCountRepository.decrement(event.productId)
            }

            try {
                cacheManager.getCache("product-info")?.evict(event.productId)
                log.debug("상품 정보 캐시 무효화: productId=${event.productId}")
            } catch (e: Exception) {
                log.warn("상품 정보 캐시 무효화 실패: productId=${event.productId}", e)
            }

            log.info("좋아요 집계 업데이트 완료: productId=${event.productId}, type=${event.type}")
        } catch (e: Exception) {
            log.error("좋아요 집계 업데이트 실패: productId=${event.productId}, type=${event.type}", e)
            // 모니터링 알람 트리거 지점 (필요시 별도 구현)
        }
    }
}
