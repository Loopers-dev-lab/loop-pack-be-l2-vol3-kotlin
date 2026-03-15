package com.loopers.application.event.product

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringProductQueryChangedEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : ProductQueryChangedEventPublisher {
    override fun publish(event: ProductQueryChangedEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
