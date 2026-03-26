package com.loopers.infrastructure.event

import com.loopers.domain.event.EventHandledModel
import com.loopers.domain.event.EventHandledRepository
import org.springframework.stereotype.Component

@Component
class EventHandledRepositoryImpl(
    private val eventHandledJpaRepository: EventHandledJpaRepository,
) : EventHandledRepository {
    override fun existsByEventId(eventId: String): Boolean {
        return eventHandledJpaRepository.existsByEventId(eventId)
    }

    override fun save(eventHandled: EventHandledModel): EventHandledModel {
        return eventHandledJpaRepository.save(eventHandled)
    }
}
