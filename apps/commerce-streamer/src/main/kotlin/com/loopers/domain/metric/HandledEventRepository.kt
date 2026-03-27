package com.loopers.domain.metric

interface HandledEventRepository {
    fun existsByEventId(eventId: Long): Boolean
    fun save(event: HandledEvent): HandledEvent
}
