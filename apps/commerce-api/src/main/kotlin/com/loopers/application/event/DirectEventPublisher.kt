package com.loopers.application.event

interface DirectEventPublisher {
    fun publish(
        topic: String,
        key: String,
        eventType: String,
        payload: Any,
    )
}
