package com.loopers.domain.order

import com.loopers.domain.order.entity.OrderItem
import com.loopers.domain.order.repository.OrderItemRepository

class FakeOrderItemRepository : OrderItemRepository {

    private val items = mutableListOf<OrderItem>()
    private var sequence = 1L

    override fun save(orderItem: OrderItem): OrderItem {
        if (orderItem.id != 0L) {
            items.removeIf { it.id == orderItem.id }
            items.add(orderItem)
        } else {
            setOrderItemId(orderItem, sequence++)
            items.add(orderItem)
        }
        return orderItem
    }

    override fun saveAll(orderItems: List<OrderItem>): List<OrderItem> {
        return orderItems.map { save(it) }
    }

    override fun findAllByOrderId(orderId: Long): List<OrderItem> {
        return items.filter { it.refOrderId == orderId }
    }

    override fun findAllByOrderIds(orderIds: List<Long>): List<OrderItem> {
        return items.filter { it.refOrderId in orderIds }
    }

    private fun setOrderItemId(orderItem: OrderItem, id: Long) {
        OrderItem::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(orderItem, id)
        }
    }
}
