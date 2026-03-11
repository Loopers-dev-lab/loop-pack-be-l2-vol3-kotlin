package com.loopers.domain.order

import java.time.LocalDate

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: Long): Order?
    fun findByUserId(userId: Long): List<Order>
    fun findByUserIdAndDateRange(userId: Long, startAt: LocalDate, endAt: LocalDate): List<Order>
    fun findAll(page: Int, size: Int): List<Order>
}
