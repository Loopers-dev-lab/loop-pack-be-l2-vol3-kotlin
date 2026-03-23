package com.loopers.domain.productlike.event

import com.loopers.domain.productlike.ProductLikeCountRepository
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory

@Component
class LikeCountEventListener(
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val cacheManager: CacheManager,
) {
    companion object {
        private val log = LoggerFactory.getLogger(LikeCountEventListener::class.java)
    }

    /**
     * 좋아요 카운트 projection 갱신 이벤트 처리 (비동기).
     * 별도 스레드에서 실행되어 좋아요 저장과 독립적이다.
     * 실패 시 로깅하고 모니터링 알람을 트리거한다.
     */
    @EventListener(LikeCountEvent::class)
    @Async("eventListenerExecutor")
    fun handleLikeCountEvent(event: LikeCountEvent) {
        try {
            updateLikeCountAndInvalidateCache(event)
            log.info("좋아요 집계 업데이트 완료: productId=${event.productId}, type=${event.type}")
        } catch (e: Exception) {
            log.error("좋아요 집계 업데이트 실패: productId=${event.productId}, type=${event.type}", e)
            // 모니터링 알람 트리거 지점 (필요시 별도 구현)
        }
    }

    @Transactional
    private fun updateLikeCountAndInvalidateCache(event: LikeCountEvent) {
        // 집계 업데이트
        when (event.type) {
            LikeCountEventType.INCREMENT -> productLikeCountRepository.increment(event.productId)
            LikeCountEventType.DECREMENT -> productLikeCountRepository.decrement(event.productId)
        }

        // 캐시 무효화
        try {
            cacheManager.getCache("product-info")?.evict(event.productId)
            log.debug("상품 정보 캐시 무효화: productId=${event.productId}")
        } catch (e: Exception) {
            log.warn("상품 정보 캐시 무효화 실패: productId=${event.productId}", e)
        }
    }
}
