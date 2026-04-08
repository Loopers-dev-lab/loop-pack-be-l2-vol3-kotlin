package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.EventHandledRecord
import org.springframework.data.jpa.repository.JpaRepository

interface EventHandledJpaRepository : JpaRepository<EventHandledRecord, Long> {
    fun existsByEventId(eventId: Long): Boolean
}
