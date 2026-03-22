package com.loopers.application.event

interface OutboxEventPublisher {
    fun publish(topic: String, key: String, payload: Map<String, Any?>)
}
