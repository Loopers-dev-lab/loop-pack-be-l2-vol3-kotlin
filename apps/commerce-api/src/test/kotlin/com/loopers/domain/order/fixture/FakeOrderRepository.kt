package com.loopers.domain.order.fixture

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository

class FakeOrderRepository : OrderRepository {

    private val store = HashMap<Long, Order>()
    private var sequence = 1L

    override fun save(order: Order): Long {
        val id = order.persistenceId ?: sequence++
        val items = order.items.map { item ->
            if (item.persistenceId != null) {
                item
            } else {
                com.loopers.domain.order.OrderItem.reconstitute(
                    persistenceId = (id * 1000) + order.items.indexOf(item),
                    productId = item.productId,
                    productName = item.productName,
                    brandName = item.brandName,
                    price = item.price,
                    quantity = item.quantity,
                )
            }
        }
        val persisted = Order.reconstitute(
            persistenceId = id,
            userId = order.userId,
            status = order.status,
            totalAmount = order.totalAmount,
            orderedAt = order.orderedAt,
            items = items,
        )
        store[id] = persisted
        return id
    }

    override fun findById(id: Long): Order? {
        return store[id]
    }

    override fun findByIdForUpdate(id: Long): Order? {
        return store[id]
    }

    override fun findAllByUserId(userId: Long): List<Order> {
        return store.values.filter { it.userId == userId }
    }

    override fun findAll(): List<Order> {
        return store.values.toList()
    }
}
