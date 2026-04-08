package com.loopers.domain.metrics

interface EventHandledRepository {
    fun existsByEventId(eventId: Long): Boolean
    fun save(record: EventHandledRecord): EventHandledRecord
}
