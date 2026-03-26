package com.loopers.application.product

import com.loopers.application.event.ProductViewedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductViewTracker(
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun track(productId: Long) {
        eventPublisher.publishEvent(ProductViewedEvent(productId = productId))
    }
}
