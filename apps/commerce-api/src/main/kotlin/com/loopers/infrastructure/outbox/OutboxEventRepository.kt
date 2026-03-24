package com.loopers.infrastructure.outbox

interface OutboxEventRepository {
    fun save(event: OutboxEvent): OutboxEvent
    fun findByPublishedAtIsNull(limit: Int = 100): List<OutboxEvent>
}
