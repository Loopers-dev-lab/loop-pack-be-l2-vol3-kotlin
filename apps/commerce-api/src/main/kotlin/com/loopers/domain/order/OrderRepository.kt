package com.loopers.domain.order

interface OrderRepository {
    fun save(order: Order): Long
    fun findById(id: Long): Order?
    fun findByIdForUpdate(id: Long): Order?
    fun findAllByUserId(userId: Long): List<Order>
    fun findAll(): List<Order>
}
