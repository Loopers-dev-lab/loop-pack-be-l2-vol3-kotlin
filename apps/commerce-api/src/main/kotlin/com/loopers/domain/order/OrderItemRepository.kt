package com.loopers.domain.order

interface OrderItemRepository {
    fun saveAll(orderItems: List<OrderItem>): List<OrderItem>
    fun findByOrderId(orderId: Long): List<OrderItem>
}
