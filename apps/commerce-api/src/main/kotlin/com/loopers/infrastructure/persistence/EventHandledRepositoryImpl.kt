package com.loopers.infrastructure.persistence

import com.loopers.domain.eventhandled.EventHandled
import com.loopers.domain.eventhandled.EventHandledRepository
import com.loopers.infrastructure.persistence.jpa.EventHandledJpaRepository
import org.springframework.stereotype.Repository

@Repository
class EventHandledRepositoryImpl(
    private val jpaRepository: EventHandledJpaRepository,
) : EventHandledRepository {
    override fun existsByDedupeKey(dedupeKey: String): Boolean {
        return jpaRepository.existsByDedupeKey(dedupeKey)
    }

    override fun save(eventHandled: EventHandled): EventHandled {
        return jpaRepository.save(eventHandled)
    }
}
