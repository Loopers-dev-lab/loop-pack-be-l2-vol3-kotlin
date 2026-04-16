package com.loopers.domain.event.handler

import com.loopers.domain.event.ProductViewedEvent
import com.loopers.infrastructure.productmetrics.ProductMetricsDailyRepository
import com.loopers.infrastructure.productmetrics.ProductMetricsRepository
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

@Component("ProductViewedEvent")
class ProductViewedEventHandler(
    private val productMetricsRepository: ProductMetricsRepository,
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
) : EventHandler {

    override fun handle(event: Any) {
        val viewedEvent = event as ProductViewedEvent
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

        productMetricsRepository.incrementViewCount(viewedEvent.productId)
        productMetricsDailyRepository.incrementViewCount(viewedEvent.productId, today)
    }
}
