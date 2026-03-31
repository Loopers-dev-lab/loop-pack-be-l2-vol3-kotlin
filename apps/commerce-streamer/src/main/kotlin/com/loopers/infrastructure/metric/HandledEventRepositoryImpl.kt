package com.loopers.infrastructure.metric

import com.loopers.domain.metric.HandledEvent
import com.loopers.domain.metric.HandledEventRepository
import org.springframework.stereotype.Repository

@Repository
class HandledEventRepositoryImpl(
    private val handledEventJpaRepository: HandledEventJpaRepository,
) : HandledEventRepository {
    override fun existsByEventId(eventId: Long): Boolean = handledEventJpaRepository.existsByEventId(eventId)

    override fun save(event: HandledEvent): HandledEvent =
        handledEventJpaRepository.saveAndFlush(
            HandledEventEntity(
                eventId = event.eventId,
                topic = event.topic,
                eventType = event.eventType,
            ),
        ).toDomain()
}
