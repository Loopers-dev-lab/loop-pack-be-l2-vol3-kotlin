package com.loopers.infrastructure.event

import com.loopers.domain.event.EventHandled
import com.loopers.domain.event.EventHandledRepository
import org.springframework.stereotype.Component

@Component
class EventHandledRepositoryImpl(
    private val jpaRepository: EventHandledJpaRepository,
) : EventHandledRepository {

    override fun existsByEventId(eventId: String): Boolean {
        return jpaRepository.existsById(eventId)
    }

    override fun save(eventHandled: EventHandled): EventHandled {
        return jpaRepository.save(eventHandled)
    }
}
