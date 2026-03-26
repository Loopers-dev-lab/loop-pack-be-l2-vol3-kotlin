package com.loopers.application.consumer

import com.loopers.infrastructure.consumer.EventHandledEntity
import com.loopers.infrastructure.consumer.EventHandledJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class EventHandledRecorder(
    private val eventHandledJpaRepository: EventHandledJpaRepository,
) {
    fun markHandled(consumerGroup: String, eventId: String): Boolean {
        return try {
            eventHandledJpaRepository.save(
                EventHandledEntity(
                    consumerGroup = consumerGroup,
                    eventId = eventId,
                ),
            )
            true
        } catch (_: DataIntegrityViolationException) {
            false
        }
    }
}
