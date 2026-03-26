package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxEventRepository
import com.loopers.domain.outbox.OutboxEventStatus
import org.springframework.stereotype.Repository

@Repository
class OutboxEventRepositoryImpl(
    private val jpaRepository: OutboxEventJpaRepository,
) : OutboxEventRepository {
    override fun save(event: OutboxEvent): Long {
        val entity = OutboxEventMapper.toEntity(event)
        val saved = jpaRepository.save(entity)
        return requireNotNull(saved.id) { "OutboxEvent 저장 실패: id가 생성되지 않았습니다." }
    }

    override fun findPendingEvents(limit: Int): List<OutboxEvent> {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, limit)
            .map { OutboxEventMapper.toDomain(it) }
    }

    override fun updateStatus(id: Long, status: OutboxEventStatus) {
        jpaRepository.updateStatus(id, status)
    }
}
