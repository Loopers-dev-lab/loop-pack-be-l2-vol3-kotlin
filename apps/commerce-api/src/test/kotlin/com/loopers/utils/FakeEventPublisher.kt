package com.loopers.utils

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher

class FakeEventPublisher : ApplicationEventPublisher {
    val events = mutableListOf<Any>()

    override fun publishEvent(event: ApplicationEvent) {
        events.add(event)
    }

    override fun publishEvent(event: Any) {
        events.add(event)
    }

    fun clear() {
        events.clear()
    }

    inline fun <reified T> findEvent(): T? = events.filterIsInstance<T>().firstOrNull()

    inline fun <reified T> findEvents(): List<T> = events.filterIsInstance<T>()

    inline fun <reified T> hasEvent(): Boolean = events.any { it is T }
}
