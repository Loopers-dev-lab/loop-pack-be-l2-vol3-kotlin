package com.loopers.domain.order

import java.time.LocalDate

interface OrderRepository {
    fun save(order: Order): Long
    fun findById(id: Long): Order?
    fun findByIdForUpdate(id: Long): Order?
    fun findAllByUserIdAndOrderedDate(userId: Long, startAt: LocalDate, endAt: LocalDate, page: Int, size: Int): List<Order>
    fun countByUserIdAndOrderedDate(userId: Long, startAt: LocalDate, endAt: LocalDate): Long
    fun findAll(): List<Order>
}
