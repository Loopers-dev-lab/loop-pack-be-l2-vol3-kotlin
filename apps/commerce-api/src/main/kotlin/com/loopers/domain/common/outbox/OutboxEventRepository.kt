package com.loopers.domain.common.outbox

interface OutboxEventRepository {
    fun save(event: OutboxEvent): OutboxEvent
    fun findUnpublished(limit: Int): List<OutboxEvent>
    fun markPublished(id: String)
}
