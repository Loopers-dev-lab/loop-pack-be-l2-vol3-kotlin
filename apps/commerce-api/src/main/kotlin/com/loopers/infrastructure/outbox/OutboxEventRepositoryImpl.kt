package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxEventRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class OutboxEventRepositoryImpl(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
) : OutboxEventRepository {

    override fun save(outboxEvent: OutboxEvent): OutboxEvent {
        return outboxEventJpaRepository.save(outboxEvent)
    }

    override fun findUnpublished(limit: Int): List<OutboxEvent> {
        return outboxEventJpaRepository.findUnpublished(PageRequest.of(0, limit))
    }
}
