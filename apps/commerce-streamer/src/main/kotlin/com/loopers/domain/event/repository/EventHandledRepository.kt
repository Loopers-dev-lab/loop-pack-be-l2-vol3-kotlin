package com.loopers.domain.event.repository

import com.loopers.domain.event.model.EventHandled

interface EventHandledRepository {
    fun existsByEventId(eventId: String): Boolean
    fun save(eventHandled: EventHandled): EventHandled
}
