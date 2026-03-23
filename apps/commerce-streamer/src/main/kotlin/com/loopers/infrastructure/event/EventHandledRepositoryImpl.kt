package com.loopers.infrastructure.event

import com.loopers.domain.event.model.EventHandled
import com.loopers.domain.event.repository.EventHandledRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface EventHandledJpaRepository : JpaRepository<EventHandledEntity, String>

@Repository
class EventHandledRepositoryImpl(
    private val eventHandledJpaRepository: EventHandledJpaRepository,
) : EventHandledRepository {

    override fun existsByEventId(eventId: String): Boolean {
        return eventHandledJpaRepository.existsById(eventId)
    }

    override fun save(eventHandled: EventHandled): EventHandled {
        return eventHandledJpaRepository.save(EventHandledEntity.fromDomain(eventHandled)).toDomain()
    }
}
