package com.loopers.domain.order

import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import java.time.ZonedDateTime

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: Long): Order?
    fun findByIdAndUserId(id: Long, userId: Long): Order?
    fun findAllByUserId(
        userId: Long,
        from: ZonedDateTime?,
        to: ZonedDateTime?,
        pageRequest: PageRequest,
    ): PageResponse<Order>
    fun findAll(pageRequest: PageRequest): PageResponse<Order>
    fun existsByIdempotencyKey(idempotencyKey: IdempotencyKey): Boolean
}
