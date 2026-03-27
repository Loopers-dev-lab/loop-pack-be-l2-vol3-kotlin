package com.loopers.application.outbox

import com.loopers.domain.common.outbox.OutboxEvent
import com.loopers.domain.common.outbox.OutboxEventRepository
import java.time.ZonedDateTime

class FakeOutboxEventRepository : OutboxEventRepository {
    private val store = mutableMapOf<String, OutboxEvent>()

    override fun save(event: OutboxEvent): OutboxEvent {
        val saved = event.copy(createdAt = ZonedDateTime.now())
        store[saved.id] = saved
        return saved
    }

    override fun findUnpublished(limit: Int): List<OutboxEvent> {
        return store.values
            .filter { it.publishedAt == null }
            .sortedBy { it.createdAt }
            .take(limit)
    }

    override fun markPublished(id: String) {
        store[id]?.let { store[id] = it.copy(publishedAt = ZonedDateTime.now()) }
    }

    fun findAll(): List<OutboxEvent> = store.values.toList()

    fun findById(id: String): OutboxEvent? = store[id]

    fun clear() {
        store.clear()
    }
}
