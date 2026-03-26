package com.loopers.domain.event.handler

import com.loopers.domain.product.event.ProductViewedEvent
import com.loopers.domain.productmetrics.ProductMetrics
import com.loopers.infrastructure.productmetrics.ProductMetricsRepository
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Component

@Component("ProductViewedEvent")
class ProductViewedEventHandler(
    private val productMetricsRepository: ProductMetricsRepository,
) : EventHandler {
    override fun handle(event: Any) {
        val viewedEvent = event as ProductViewedEvent

        val metrics = productMetricsRepository.findByProductId(viewedEvent.productId)
        if (metrics == null) {
            val newMetrics = ProductMetrics.create(viewedEvent.productId)
            productMetricsRepository.save(newMetrics)
        }

        productMetricsRepository.incrementViewCount(viewedEvent.productId)
    }
}
