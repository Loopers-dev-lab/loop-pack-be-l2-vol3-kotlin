package com.loopers.application.outbox

interface OutboxEventPublisher {
    fun publish(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        payload: Map<String, Any?>,
        partitionKey: String,
        topic: String,
    )
}
