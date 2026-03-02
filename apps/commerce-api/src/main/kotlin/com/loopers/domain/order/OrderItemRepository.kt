package com.loopers.domain.order

interface OrderItemRepository {
    fun saveAll(items: List<OrderItem>): List<OrderItem>
    fun findByOrderId(orderId: Long): List<OrderItem>
    fun findByOrderIds(orderIds: List<Long>): List<OrderItem>
}
