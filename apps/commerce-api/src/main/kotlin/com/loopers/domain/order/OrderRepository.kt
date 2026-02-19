package com.loopers.domain.order

import com.loopers.domain.PageResult
import java.time.ZonedDateTime

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: Long): Order?
    fun findAllByUserId(userId: Long, from: ZonedDateTime, to: ZonedDateTime, page: Int, size: Int): PageResult<Order>
    fun findAll(page: Int, size: Int): PageResult<Order>
}
