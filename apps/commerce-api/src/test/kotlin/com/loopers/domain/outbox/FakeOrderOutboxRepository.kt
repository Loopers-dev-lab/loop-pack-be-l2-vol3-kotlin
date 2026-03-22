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

    override fun findAllUnpublished(): List<OrderOutbox> {
        return store.filter { !it.published }
    }

    private fun setId(entity: OrderOutbox, id: Long) {
        OrderOutbox::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
