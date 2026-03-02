package com.loopers.domain.order

interface OrderItemRepository {
    fun saveAll(items: List<OrderItemModel>): List<OrderItemModel>
    fun findAllByOrderId(orderId: Long): List<OrderItemModel>
}
