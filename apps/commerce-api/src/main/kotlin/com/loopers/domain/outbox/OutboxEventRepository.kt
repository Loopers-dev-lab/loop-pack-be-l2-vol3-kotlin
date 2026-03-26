package com.loopers.domain.outbox

interface OutboxEventRepository {
    fun save(outboxEvent: OutboxEvent): OutboxEvent
    fun findPendingEvents(limit: Int): List<OutboxEvent>
}
