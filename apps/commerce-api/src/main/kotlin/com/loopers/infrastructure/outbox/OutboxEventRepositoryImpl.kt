package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxEventRepository
import com.loopers.domain.outbox.OutboxStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class OutboxEventRepositoryImpl(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
) : OutboxEventRepository {

    override fun save(outboxEvent: OutboxEvent): OutboxEvent {
        return outboxEventJpaRepository.save(outboxEvent)
    }

    override fun findPendingEvents(limit: Int): List<OutboxEvent> {
        return outboxEventJpaRepository.findByStatusOrderByCreatedAtAsc(
            OutboxStatus.PENDING,
            PageRequest.of(0, limit),
        )
    }
}
