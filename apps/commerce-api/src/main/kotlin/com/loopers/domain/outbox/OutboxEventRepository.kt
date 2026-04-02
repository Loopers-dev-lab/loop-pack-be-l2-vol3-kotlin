package com.loopers.domain.outbox

interface OutboxEventRepository {
    fun save(outboxEvent: OutboxEvent): OutboxEvent
    fun findUnpublished(limit: Int): List<OutboxEvent>
}
