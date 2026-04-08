package com.loopers.infrastructure.event

import com.loopers.domain.event.OutboxEvent
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventJpaRepository : JpaRepository<OutboxEvent, Long> {
    fun findTop100ByPublishedFalseOrderByCreatedAtAsc(): List<OutboxEvent>
}
