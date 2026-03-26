package com.loopers.domain.outbox

interface OutboxEventRepository {
    fun save(event: OutboxEvent): Long
    fun findPendingEvents(limit: Int): List<OutboxEvent>
    fun updateStatus(id: Long, status: OutboxEventStatus)
}
