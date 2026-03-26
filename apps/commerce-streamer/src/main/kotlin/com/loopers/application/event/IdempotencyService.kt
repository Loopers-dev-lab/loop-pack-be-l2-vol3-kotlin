package com.loopers.application.event

import com.loopers.infrastructure.event.EventHandledEntity
import com.loopers.infrastructure.event.EventHandledJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class IdempotencyService(
    private val eventHandledJpaRepository: EventHandledJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isAlreadyHandled(eventId: String): Boolean {
        return eventHandledJpaRepository.existsByEventId(eventId)
    }

    fun markHandled(eventId: String, eventType: String) {
        try {
            eventHandledJpaRepository.save(EventHandledEntity(eventId = eventId, eventType = eventType))
        } catch (e: DataIntegrityViolationException) {
            log.warn("이미 처리된 이벤트입니다. eventId={}", eventId)
        }
    }
}
