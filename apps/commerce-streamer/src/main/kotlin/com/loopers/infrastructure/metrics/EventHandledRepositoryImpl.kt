package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.EventHandledRecord
import com.loopers.domain.metrics.EventHandledRepository
import org.springframework.stereotype.Repository

@Repository
class EventHandledRepositoryImpl(
    private val eventHandledJpaRepository: EventHandledJpaRepository,
) : EventHandledRepository {
    override fun existsByEventId(eventId: Long): Boolean {
        return eventHandledJpaRepository.existsByEventId(eventId)
    }

    override fun save(record: EventHandledRecord): EventHandledRecord {
        return eventHandledJpaRepository.save(record)
    }
}
