package com.loopers.domain.order

import com.loopers.support.PageResult
import java.time.LocalDate

interface OrderRepository {
    fun save(order: Order): Order
    fun findByIdOrNull(id: Long): Order?
    fun findByUserIdAndDateRange(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        page: Int,
        size: Int,
    ): PageResult<Order>
    fun findAll(page: Int, size: Int): PageResult<Order>
}
