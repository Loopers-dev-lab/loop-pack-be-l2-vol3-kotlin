package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventJpaRepository : JpaRepository<OutboxEvent, Long> {
    fun findByStatusOrderByCreatedAtAsc(status: OutboxStatus, pageable: Pageable): List<OutboxEvent>
}
