package com.loopers.application.catalog.event

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class CatalogEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    @Async
    fun handleProductViewed(event: ProductViewedEvent) {
        log.info("[UserAction] PRODUCT_VIEWED: userId=${event.userId}, productId=${event.productId}")
    }
}
