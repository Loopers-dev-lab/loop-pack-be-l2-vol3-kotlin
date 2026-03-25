package com.loopers.domain.event.handler

import com.loopers.domain.product.event.ProductViewedEvent
import com.loopers.domain.productmetrics.ProductMetrics
import com.loopers.domain.productmetrics.ProductMetricsRepository
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Component

@Component("ProductViewedEvent")
class ProductViewedEventHandler(
    private val productMetricsRepository: ProductMetricsRepository,
) : EventHandler {
    override fun handle(event: Any) {
        val viewedEvent = event as ProductViewedEvent

        var metrics = productMetricsRepository.findByProductIdWithLock(viewedEvent.productId)
        if (metrics == null) {
            metrics = ProductMetrics.create(viewedEvent.productId)
        }

        metrics.incrementViewCount()
        productMetricsRepository.save(metrics)
    }
}
