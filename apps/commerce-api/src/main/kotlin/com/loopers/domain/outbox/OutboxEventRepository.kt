package com.loopers.domain.outbox

import java.time.ZonedDateTime

interface OutboxEventRepository {
    fun save(outboxEvent: OutboxEvent): OutboxEvent
    fun findAllByStatus(status: OutboxStatus): List<OutboxEvent>
    fun findByEventId(eventId: String): OutboxEvent?
    fun findAllByStatusAndCreatedAtBefore(status: OutboxStatus, threshold: ZonedDateTime): List<OutboxEvent>
}
