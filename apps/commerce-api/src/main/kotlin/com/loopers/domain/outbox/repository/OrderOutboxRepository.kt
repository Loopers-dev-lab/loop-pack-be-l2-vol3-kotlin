package com.loopers.domain.outbox.repository

import com.loopers.domain.outbox.model.OrderOutbox

interface OrderOutboxRepository {
    fun save(outbox: OrderOutbox): OrderOutbox
    fun saveAll(outboxes: List<OrderOutbox>): List<OrderOutbox>
    fun findAllUnpublished(limit: Int = 100): List<OrderOutbox>
}
