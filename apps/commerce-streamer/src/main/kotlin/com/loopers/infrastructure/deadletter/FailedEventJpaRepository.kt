package com.loopers.infrastructure.deadletter

import com.loopers.domain.deadletter.FailedEvent
import com.loopers.domain.deadletter.FailedEventStatus
import org.springframework.data.jpa.repository.JpaRepository

interface FailedEventJpaRepository : JpaRepository<FailedEvent, Long> {
    fun findTop100ByStatusOrderByCreatedAtAsc(status: FailedEventStatus): List<FailedEvent>
}
