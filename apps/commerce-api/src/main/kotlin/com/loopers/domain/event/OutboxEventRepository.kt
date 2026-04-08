package com.loopers.domain.event

interface OutboxEventRepository {
    fun save(outboxEvent: OutboxEvent): OutboxEvent
    fun findUnpublishedEvents(limit: Int): List<OutboxEvent>
    fun markPublished(id: Long)
}
