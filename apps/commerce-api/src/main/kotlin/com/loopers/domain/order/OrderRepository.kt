package com.loopers.domain.order

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import java.time.LocalDateTime

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(orderId: Long): Order?
    fun findByUserIdAndCreatedAtBetween(userId: Long, startAt: LocalDateTime, endAt: LocalDateTime): List<Order>
    fun findAll(pageQuery: PageQuery): PageResult<Order>
}
