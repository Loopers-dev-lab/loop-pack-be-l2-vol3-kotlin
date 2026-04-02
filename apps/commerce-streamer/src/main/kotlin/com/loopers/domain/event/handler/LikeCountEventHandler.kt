package com.loopers.domain.event.handler

import com.loopers.domain.productlike.ProductLikeCountRepository
import com.loopers.domain.event.LikeCountEvent
import com.loopers.domain.event.LikeCountEventType
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Component

@Component("LikeCountEvent")
class LikeCountEventHandler(
    private val productLikeCountRepository: ProductLikeCountRepository,
) : EventHandler {
    override fun handle(event: Any) {
        val likeEvent = event as LikeCountEvent

        when (likeEvent.type) {
            LikeCountEventType.INCREMENT -> productLikeCountRepository.increment(likeEvent.productId)
            LikeCountEventType.DECREMENT -> productLikeCountRepository.decrement(likeEvent.productId)
        }
    }
}
