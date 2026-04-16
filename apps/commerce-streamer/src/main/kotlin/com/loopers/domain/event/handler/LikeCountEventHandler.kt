package com.loopers.domain.event.handler

import com.loopers.domain.productlike.ProductLikeCountRepository
import com.loopers.domain.event.LikeCountEvent
import com.loopers.domain.event.LikeCountEventType
import com.loopers.infrastructure.productmetrics.ProductMetricsDailyRepository
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

@Component("LikeCountEvent")
class LikeCountEventHandler(
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
) : EventHandler {

    override fun handle(event: Any) {
        val likeEvent = event as LikeCountEvent
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val delta = when (likeEvent.type) {
            LikeCountEventType.INCREMENT -> 1L
            LikeCountEventType.DECREMENT -> -1L
        }

        when (likeEvent.type) {
            LikeCountEventType.INCREMENT -> productLikeCountRepository.increment(likeEvent.productId)
            LikeCountEventType.DECREMENT -> productLikeCountRepository.decrement(likeEvent.productId)
        }
        productMetricsDailyRepository.incrementLikeCount(likeEvent.productId, delta, today)
    }
}
