package com.loopers.domain.order

interface OrderItemRepository {
    fun findAllByOrderId(orderId: Long): List<OrderItemModel>
    fun findAllByOrderIdIn(orderIds: List<Long>): List<OrderItemModel>
    fun save(item: OrderItemModel): OrderItemModel
    fun saveAll(items: List<OrderItemModel>): List<OrderItemModel>
}
