package com.loopers.domain.event.handler

import com.loopers.domain.productlike.event.LikeCountEvent
import com.loopers.domain.productlike.event.LikeCountEventType
import com.loopers.domain.productmetrics.ProductMetrics
import com.loopers.domain.productmetrics.ProductMetricsRepository
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Component

@Component("LikeCountEvent")
class LikeCountEventHandler(
    private val productMetricsRepository: ProductMetricsRepository,
) : EventHandler {
    override fun handle(event: Any) {
        val likeEvent = event as LikeCountEvent

        var metrics = productMetricsRepository.findByProductIdWithLock(likeEvent.productId)
        if (metrics == null) {
            metrics = ProductMetrics.create(likeEvent.productId)
        }

        when (likeEvent.type) {
            LikeCountEventType.INCREMENT -> metrics.incrementLikeCount()
            LikeCountEventType.DECREMENT -> metrics.decrementLikeCount()
        }

        productMetricsRepository.save(metrics)
    }
}
