package com.loopers.application.event

import com.loopers.domain.event.EventHandled
import com.loopers.domain.event.EventHandledRepository
import org.springframework.stereotype.Component

@Component
class IdempotencyService(
    private val eventHandledRepository: EventHandledRepository,
) {

    fun isAlreadyHandled(eventId: String): Boolean {
        return eventHandledRepository.existsByEventId(eventId)
    }

    fun markHandled(eventId: String, aggregateType: String, aggregateId: String, eventType: String) {
        eventHandledRepository.save(
            EventHandled(
                eventId = eventId,
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
            ),
        )
    }
}
