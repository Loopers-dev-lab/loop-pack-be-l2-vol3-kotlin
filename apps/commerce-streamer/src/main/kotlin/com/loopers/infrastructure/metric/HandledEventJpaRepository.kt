package com.loopers.infrastructure.metric

import org.springframework.data.jpa.repository.JpaRepository

interface HandledEventJpaRepository : JpaRepository<HandledEventEntity, Long> {
    fun existsByEventId(eventId: Long): Boolean
}
