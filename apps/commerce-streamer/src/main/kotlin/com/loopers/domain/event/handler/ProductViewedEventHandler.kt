package com.loopers.domain.event.handler

import com.loopers.domain.event.ProductViewedEvent
import com.loopers.infrastructure.productmetrics.ProductMetricsRepository
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Component

@Component("ProductViewedEvent")
class ProductViewedEventHandler(
    private val productMetricsRepository: ProductMetricsRepository,
) : EventHandler {
    override fun handle(event: Any) {
        val viewedEvent = event as ProductViewedEvent
        productMetricsRepository.incrementViewCount(viewedEvent.productId)
    }
}
