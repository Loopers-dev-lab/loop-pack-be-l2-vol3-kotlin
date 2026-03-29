package com.loopers.domain.event

import com.loopers.domain.event.model.EventHandled
import com.loopers.domain.event.repository.EventHandledRepository

class FakeEventHandledRepository : EventHandledRepository {

    private val store = mutableListOf<EventHandled>()

    override fun existsByEventId(eventId: String): Boolean {
        return store.any { it.eventId == eventId }
    }

    override fun save(eventHandled: EventHandled): EventHandled {
        store.add(eventHandled)
        return eventHandled
    }
}
