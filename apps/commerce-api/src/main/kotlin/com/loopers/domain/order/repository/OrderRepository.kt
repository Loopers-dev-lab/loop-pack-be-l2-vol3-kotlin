package com.loopers.domain.order.repository

import com.loopers.domain.PageResult
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.model.Order
import java.time.ZonedDateTime

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: OrderId): Order?
    fun findAllByUserId(userId: UserId, from: ZonedDateTime, to: ZonedDateTime, page: Int, size: Int): PageResult<Order>
    fun findAll(page: Int, size: Int): PageResult<Order>
}
