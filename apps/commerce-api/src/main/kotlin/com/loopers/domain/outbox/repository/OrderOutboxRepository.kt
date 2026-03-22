package com.loopers.domain.outbox.repository

import com.loopers.domain.outbox.model.OrderOutbox

interface OrderOutboxRepository {
    fun save(outbox: OrderOutbox): OrderOutbox
    fun findAllUnpublished(): List<OrderOutbox>
}
