package com.loopers.infrastructure.persistence.jpa

import com.loopers.domain.eventhandled.EventHandled
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventHandledJpaRepository : JpaRepository<EventHandled, Long> {
    fun existsByDedupeKey(dedupeKey: String): Boolean
}
