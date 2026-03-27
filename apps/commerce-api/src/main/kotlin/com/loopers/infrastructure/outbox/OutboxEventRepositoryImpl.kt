package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxEventRepository
import com.loopers.domain.outbox.OutboxStatus
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OutboxEventRepositoryImpl(
    private val jpaRepository: OutboxEventJpaRepository,
) : OutboxEventRepository {

    override fun save(outboxEvent: OutboxEvent): OutboxEvent {
        return jpaRepository.save(outboxEvent)
    }

    override fun findAllByStatus(status: OutboxStatus): List<OutboxEvent> {
        return jpaRepository.findAllByStatus(status)
    }

    override fun findByEventId(eventId: String): OutboxEvent? {
        return jpaRepository.findByEventId(eventId)
    }

    override fun findAllByStatusAndCreatedAtBefore(status: OutboxStatus, threshold: ZonedDateTime): List<OutboxEvent> {
        return jpaRepository.findAllByStatusAndCreatedAtBefore(status, threshold)
    }
}
