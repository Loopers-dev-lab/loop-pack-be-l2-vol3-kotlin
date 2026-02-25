package com.loopers.application.order

import com.loopers.domain.order.model.Order
import com.loopers.domain.order.model.OrderItem
import com.loopers.domain.order.repository.OrderItemRepository

fun OrderItemRepository.findGroupedByOrderIds(orders: List<Order>): Map<Long, List<OrderItem>> {
    if (orders.isEmpty()) return emptyMap()
    return this.findAllByOrderIds(orders.map { it.id })
        .groupBy { it.refOrderId }
}
