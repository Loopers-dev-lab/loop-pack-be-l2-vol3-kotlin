package com.loopers.infrastructure.outbox

import com.loopers.domain.common.outbox.OutboxEvent
import com.loopers.domain.common.outbox.OutboxEventRepository
import org.springframework.stereotype.Component

@Component
class OutboxEventRepositoryImpl(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
) : OutboxEventRepository {

    override fun save(event: OutboxEvent): OutboxEvent {
        return outboxEventJpaRepository.save(OutboxEventJpaModel.from(event)).toModel()
    }

    override fun findUnpublished(limit: Int): List<OutboxEvent> {
        return outboxEventJpaRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()
            .take(limit)
            .map { it.toModel() }
    }

    override fun markPublished(id: String) {
        outboxEventJpaRepository.findById(id).ifPresent { entity ->
            entity.markPublished()
            outboxEventJpaRepository.save(entity)
        }
    }
}
