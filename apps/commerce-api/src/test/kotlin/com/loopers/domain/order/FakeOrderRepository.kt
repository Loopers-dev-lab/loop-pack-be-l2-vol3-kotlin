package com.loopers.domain.order

import com.loopers.domain.PageResult
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.model.Order
import com.loopers.domain.order.repository.OrderRepository
import java.time.ZonedDateTime

class FakeOrderRepository : OrderRepository {

    private val orders = mutableListOf<Order>()
    private val savedAt = mutableMapOf<Long, ZonedDateTime>()
    private var sequence = 1L

    override fun save(order: Order): Order {
        if (order.id.value != 0L) {
            orders.removeIf { it.id == order.id }
            orders.add(order)
            savedAt.putIfAbsent(order.id.value, ZonedDateTime.now())
        } else {
            setOrderId(order, sequence++)
            orders.add(order)
            savedAt[order.id.value] = ZonedDateTime.now()
        }
        return order
    }

    override fun findById(id: OrderId): Order? {
        return orders.find { it.id == id }
    }

    override fun findAllByUserId(
        userId: UserId,
        from: ZonedDateTime,
        to: ZonedDateTime,
        page: Int,
        size: Int,
    ): PageResult<Order> {
        val filtered = orders.filter { order ->
            order.refUserId == userId &&
                savedAt[order.id.value]?.let { !it.isBefore(from) && !it.isAfter(to) } ?: false
        }
        val offset = page * size
        val content = filtered.drop(offset).take(size)
        return PageResult(content, filtered.size.toLong(), page, size)
    }

    override fun findAll(page: Int, size: Int): PageResult<Order> {
        val offset = page * size
        val content = orders.drop(offset).take(size)
        return PageResult(content, orders.size.toLong(), page, size)
    }

    private fun setOrderId(order: Order, id: Long) {
        Order::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(order, id)
        }
    }
}
