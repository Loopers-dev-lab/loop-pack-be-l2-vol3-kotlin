package com.loopers.domain.event

interface EventHandledRepository {
    fun existsByEventId(eventId: String): Boolean
    fun save(eventHandled: EventHandledModel): EventHandledModel
}
