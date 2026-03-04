package com.loopers.domain.order

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: Long): Order?
    fun findAllByMemberId(memberId: Long): List<Order>
}
