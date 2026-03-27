package com.loopers.domain.eventhandled

interface EventHandledRepository {

    fun existsByDedupeKey(dedupeKey: String): Boolean

    fun save(eventHandled: EventHandledDto): EventHandledDto
}
