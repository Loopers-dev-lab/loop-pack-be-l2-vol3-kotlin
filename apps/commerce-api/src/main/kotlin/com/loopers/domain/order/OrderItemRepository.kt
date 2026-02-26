package com.loopers.domain.order

interface OrderItemRepository {
    fun findAllByOrderId(orderId: Long): List<OrderItemModel>
}
