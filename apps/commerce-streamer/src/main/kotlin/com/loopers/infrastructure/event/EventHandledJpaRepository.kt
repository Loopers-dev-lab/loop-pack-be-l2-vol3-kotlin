package com.loopers.infrastructure.event

import com.loopers.domain.event.EventHandledModel
import org.springframework.data.jpa.repository.JpaRepository

interface EventHandledJpaRepository : JpaRepository<EventHandledModel, Long> {
    fun existsByEventId(eventId: String): Boolean
}
