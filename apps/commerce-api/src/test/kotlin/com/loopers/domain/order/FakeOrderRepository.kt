package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.PageResult
import com.loopers.domain.order.entity.Order
import com.loopers.domain.order.repository.OrderRepository
import java.time.ZonedDateTime

class FakeOrderRepository : OrderRepository {

    private val orders = mutableListOf<Order>()
    private var sequence = 1L

    override fun save(order: Order): Order {
        if (order.id != 0L) {
            orders.removeIf { it.id == order.id }
            orders.add(order)
        } else {
            setEntityId(order, sequence++)
            orders.add(order)
        }
        return order
    }

    override fun findById(id: Long): Order? {
        return orders.find { it.id == id }
    }

    override fun findAllByUserId(
        userId: Long,
        from: ZonedDateTime,
        to: ZonedDateTime,
        page: Int,
        size: Int,
    ): PageResult<Order> {
        val filtered = orders.filter {
            it.refUserId == userId &&
                    !it.createdAt.isBefore(from) &&
                    !it.createdAt.isAfter(to)
        }.sortedByDescending { it.createdAt }
        val offset = page * size
        val content = filtered.drop(offset).take(size)
        return PageResult(content, filtered.size.toLong(), page, size)
    }

    override fun findAll(page: Int, size: Int): PageResult<Order> {
        val offset = page * size
        val content = orders.drop(offset).take(size)
        return PageResult(content, orders.size.toLong(), page, size)
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        BaseEntity::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
