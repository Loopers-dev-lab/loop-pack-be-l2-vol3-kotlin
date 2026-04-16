package com.loopers.domain.event.handler

import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.infrastructure.productmetrics.ProductMetricsDailyRepository
import com.loopers.infrastructure.productmetrics.ProductMetricsRepository
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

@Component("OrderCreatedEvent")
class OrderCreatedEventHandler(
    private val productMetricsRepository: ProductMetricsRepository,
    private val productMetricsDailyRepository: ProductMetricsDailyRepository,
) : EventHandler {

    override fun handle(event: Any) {
        val orderEvent = event as OrderCreatedEvent
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

        for (lineItem in orderEvent.lineItems) {
            productMetricsRepository.incrementSalesCount(lineItem.productId, lineItem.quantity)
            productMetricsDailyRepository.incrementSalesCount(lineItem.productId, lineItem.quantity.toLong(), today)
        }
    }
}
