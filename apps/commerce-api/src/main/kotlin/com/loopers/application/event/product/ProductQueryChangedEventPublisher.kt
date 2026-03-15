package com.loopers.application.event.product

interface ProductQueryChangedEventPublisher {
    fun publish(event: ProductQueryChangedEvent)
}
