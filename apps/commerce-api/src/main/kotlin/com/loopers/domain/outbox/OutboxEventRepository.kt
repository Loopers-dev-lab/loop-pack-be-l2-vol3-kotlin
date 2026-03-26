package com.loopers.domain.outbox

interface OutboxEventRepository {
    fun save(outboxEvent: OutboxEventModel): OutboxEventModel
    fun findPendingEvents(): List<OutboxEventModel>
}
