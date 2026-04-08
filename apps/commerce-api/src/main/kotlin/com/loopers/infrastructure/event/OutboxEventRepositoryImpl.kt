package com.loopers.infrastructure.event

import com.loopers.domain.event.OutboxEvent
import com.loopers.domain.event.OutboxEventRepository
import org.springframework.stereotype.Repository

@Repository
class OutboxEventRepositoryImpl(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
) : OutboxEventRepository {
    override fun save(outboxEvent: OutboxEvent): OutboxEvent {
        return outboxEventJpaRepository.save(outboxEvent)
    }

    override fun findUnpublishedEvents(limit: Int): List<OutboxEvent> {
        return outboxEventJpaRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc()
    }

    override fun markPublished(id: Long) {
        outboxEventJpaRepository.findById(id).ifPresent { it.markPublished() }
    }
}
