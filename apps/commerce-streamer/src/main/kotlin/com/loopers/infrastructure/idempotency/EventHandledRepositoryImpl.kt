package com.loopers.infrastructure.idempotency

import com.loopers.domain.idempotency.EventHandled
import com.loopers.domain.idempotency.EventHandledRepository
import org.springframework.stereotype.Repository

@Repository
class EventHandledRepositoryImpl(
    private val eventHandledJpaRepository: EventHandledJpaRepository,
) : EventHandledRepository {

    override fun existsByEventId(eventId: String): Boolean {
        return eventHandledJpaRepository.existsById(eventId)
    }

    override fun save(eventHandled: EventHandled): EventHandled {
        return eventHandledJpaRepository.save(eventHandled)
    }
}
