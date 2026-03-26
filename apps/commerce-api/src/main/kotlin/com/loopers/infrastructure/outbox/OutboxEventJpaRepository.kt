package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEventStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface OutboxEventJpaRepository : JpaRepository<OutboxEventEntity, Long> {
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.status = :status ORDER BY e.createdAt ASC LIMIT :limit")
    fun findByStatusOrderByCreatedAtAsc(status: OutboxEventStatus, limit: Int): List<OutboxEventEntity>

    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.status = :status WHERE e.id = :id")
    fun updateStatus(id: Long, status: OutboxEventStatus)
}
