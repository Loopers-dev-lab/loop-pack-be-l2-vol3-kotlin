package com.loopers.domain.outbox

interface OutboxEventRepository {
    fun save(event: OutboxEvent): Long
    fun findPendingEventsForUpdate(limit: Int): List<OutboxEvent>
    fun findFailedEventsForUpdate(limit: Int): List<OutboxEvent>
    fun markAsSending(ids: List<Long>)
    fun updateStatus(id: Long, status: OutboxEventStatus)
    fun updateStatusAndRetryCount(id: Long, status: OutboxEventStatus, retryCount: Int)
    fun deletePublishedBefore(hours: Int): Int
}
