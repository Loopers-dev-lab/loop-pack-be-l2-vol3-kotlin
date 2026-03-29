package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface OutboxEventJpaRepository : JpaRepository<OutboxEvent, Long> {
    fun findAllByStatus(status: OutboxStatus): List<OutboxEvent>
    fun findByEventId(eventId: String): OutboxEvent?
    fun findAllByStatusAndCreatedAtBefore(status: OutboxStatus, threshold: ZonedDateTime): List<OutboxEvent>
}
