package com.loopers.domain.productlike.event

import com.loopers.domain.productlike.ProductLikeCountRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeCountEventListener(
    private val productLikeCountRepository: ProductLikeCountRepository,
) {

    /**
     * 좋아요 카운트 이벤트 처리
     * @EventListener는 동기적으로 처리되며, @Transactional로 새로운 트랜잭션 시작
     */
    @EventListener(LikeCountEvent::class)
    @Transactional
    fun handleLikeCountEvent(event: LikeCountEvent) {
        when (event.type) {
            LikeCountEventType.INCREMENT -> productLikeCountRepository.increment(event.productId)
            LikeCountEventType.DECREMENT -> productLikeCountRepository.decrement(event.productId)
        }
    }
}
