package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEventModel
import com.loopers.domain.outbox.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventJpaRepository : JpaRepository<OutboxEventModel, Long> {
    fun findTop100ByStatusOrderByCreatedAtAsc(status: OutboxStatus): List<OutboxEventModel>
}
