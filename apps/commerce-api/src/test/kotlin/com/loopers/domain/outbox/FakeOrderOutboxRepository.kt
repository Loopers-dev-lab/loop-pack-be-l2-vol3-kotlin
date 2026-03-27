package com.loopers.domain.outbox

import com.loopers.domain.outbox.model.OrderOutbox
import com.loopers.domain.outbox.repository.OrderOutboxRepository

class FakeOrderOutboxRepository : OrderOutboxRepository {

    private val store = mutableListOf<OrderOutbox>()
    private var sequence = 1L

    override fun save(outbox: OrderOutbox): OrderOutbox {
        if (outbox.id != 0L) {
            store.removeIf { it.id == outbox.id }
            store.add(outbox)
        } else {
            setId(outbox, sequence++)
            store.add(outbox)
        }
        return outbox
    }

    override fun saveAll(outboxes: List<OrderOutbox>): List<OrderOutbox> {
        return outboxes.map { save(it) }
    }

    override fun findAllUnpublished(limit: Int): List<OrderOutbox> {
        return store.filter { !it.published }.take(limit)
    }

    private fun setId(entity: OrderOutbox, id: Long) {
        OrderOutbox::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
