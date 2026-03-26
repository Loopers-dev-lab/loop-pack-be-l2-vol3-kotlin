package com.loopers.domain.event.handler

import com.loopers.domain.order.event.OrderCreatedEvent
import com.loopers.infrastructure.productmetrics.ProductMetricsRepository
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Component

@Component("OrderCreatedEvent")
class OrderCreatedEventHandler(
    private val productMetricsRepository: ProductMetricsRepository,
) : EventHandler {
    override fun handle(event: Any) {
        val orderEvent = event as OrderCreatedEvent

        for (lineItem in orderEvent.lineItems) {
            productMetricsRepository.incrementSalesCount(lineItem.productId, lineItem.quantity)
        }
    }
}
