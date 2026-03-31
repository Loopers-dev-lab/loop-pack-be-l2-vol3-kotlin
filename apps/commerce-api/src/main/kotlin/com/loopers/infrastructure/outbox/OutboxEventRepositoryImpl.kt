package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEventModel
import com.loopers.domain.outbox.OutboxEventRepository
import com.loopers.domain.outbox.OutboxStatus
import org.springframework.stereotype.Component

@Component
class OutboxEventRepositoryImpl(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
) : OutboxEventRepository {
    override fun save(outboxEvent: OutboxEventModel): OutboxEventModel {
        return outboxEventJpaRepository.save(outboxEvent)
    }

    override fun findPendingEvents(): List<OutboxEventModel> {
        return outboxEventJpaRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)
    }

    override fun findFailedEvents(): List<OutboxEventModel> {
        return outboxEventJpaRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.FAILED)
    }
}
