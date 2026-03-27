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

    override fun findPendingEventsForUpdate(limit: Int): List<OutboxEvent> {
        return jpaRepository.findByStatusForUpdate(OutboxEventStatus.PENDING, limit)
            .map { OutboxEventMapper.toDomain(it) }
    }

    override fun findFailedEventsForUpdate(limit: Int): List<OutboxEvent> {
        return jpaRepository.findByStatusForUpdate(OutboxEventStatus.FAILED, limit)
            .map { OutboxEventMapper.toDomain(it) }
    }

    override fun markAsSending(ids: List<Long>) {
        jpaRepository.markAsSending(ids)
    }

    override fun updateStatus(id: Long, status: OutboxEventStatus) {
        jpaRepository.updateStatus(id, status)
    }

    override fun updateStatusAndRetryCount(id: Long, status: OutboxEventStatus, retryCount: Int) {
        jpaRepository.updateStatusAndRetryCount(id, status, retryCount)
    }

    override fun deletePublishedBefore(hours: Int): Int {
        return jpaRepository.deletePublishedBefore(hours)
    }

    override fun recoverStuckSending(minutes: Int): Int {
        return jpaRepository.recoverStuckSending(minutes)
    }

    override fun batchUpdateStatus(ids: List<Long>, status: OutboxEventStatus) {
        jpaRepository.batchUpdateStatus(ids, status)
    }
}
