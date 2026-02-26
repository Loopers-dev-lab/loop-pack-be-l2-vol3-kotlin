package com.loopers.domain.order.repository

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.order.model.OrderItem

interface OrderItemRepository {
    fun save(orderItem: OrderItem): OrderItem
    fun saveAll(orderItems: List<OrderItem>): List<OrderItem>
    fun findAllByOrderId(orderId: OrderId): List<OrderItem>
    fun findAllByOrderIds(orderIds: List<OrderId>): List<OrderItem>
}
