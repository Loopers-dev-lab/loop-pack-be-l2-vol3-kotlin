package com.loopers.infrastructure.outbox

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class OutboxEventRepositoryImpl(
    private val outboxJpaRepository: OutboxJpaRepository,
) : OutboxEventRepository {

    override fun save(event: OutboxEvent): OutboxEvent {
        return outboxJpaRepository.save(event)
    }

    override fun saveAll(events: List<OutboxEvent>) {
        outboxJpaRepository.saveAll(events)
    }

    override fun findByPublishedAtIsNull(limit: Int): List<OutboxEvent> {
        return outboxJpaRepository.findByPublishedAtIsNull(PageRequest.of(0, limit))
    }
}
