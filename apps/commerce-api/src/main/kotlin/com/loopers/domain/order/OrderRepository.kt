package com.loopers.domain.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: Long): Order?
    fun findByUserId(userId: Long, pageable: Pageable): Page<Order>
    fun findOrders(pageable: Pageable): Page<Order>
}
