package com.loopers.application.event

class FakeOutboxEventPublisher : OutboxEventPublisher {

    val published = mutableListOf<Triple<String, String, Map<String, Any?>>>()

    override fun publish(topic: String, key: String, payload: Map<String, Any?>) {
        published.add(Triple(topic, key, payload))
    }
}
