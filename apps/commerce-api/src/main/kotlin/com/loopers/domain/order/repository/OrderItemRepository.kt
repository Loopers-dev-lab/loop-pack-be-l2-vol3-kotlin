package com.loopers.domain.order.repository

import com.loopers.domain.order.model.OrderItem

interface OrderItemRepository {
    fun save(orderItem: OrderItem): OrderItem
    fun saveAll(orderItems: List<OrderItem>): List<OrderItem>
    fun findAllByOrderId(orderId: Long): List<OrderItem>
    fun findAllByOrderIds(orderIds: List<Long>): List<OrderItem>
}
