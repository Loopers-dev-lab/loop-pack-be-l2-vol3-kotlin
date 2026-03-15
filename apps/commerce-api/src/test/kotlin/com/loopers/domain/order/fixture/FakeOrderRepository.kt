package com.loopers.domain.order.fixture

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import java.time.LocalDate
import java.time.ZoneId

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
                    refProductId = item.refProductId,
                    productName = item.productName,
                    brandName = item.brandName,
                    price = item.price,
                    quantity = item.quantity,
                )
            }
        }
        val persisted = Order.reconstitute(
            persistenceId = id,
            refUserId = order.refUserId,
            refUserCouponId = order.refUserCouponId,
            status = order.status,
            originalAmount = order.originalAmount,
            discountAmount = order.discountAmount,
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

    override fun findAllByUserIdAndOrderedDate(
        userId: Long,
        startAt: LocalDate,
        endAt: LocalDate,
        page: Int,
        size: Int,
    ): List<Order> {
        val zone = ZoneId.systemDefault()
        val startDateTime = startAt.atStartOfDay(zone)
        val endDateTime = endAt.plusDays(1).atStartOfDay(zone)
        return store.values
            .filter { it.refUserId == userId && it.orderedAt >= startDateTime && it.orderedAt < endDateTime }
            .sortedByDescending { it.orderedAt }
            .drop(page * size)
            .take(size)
    }

    override fun countByUserIdAndOrderedDate(userId: Long, startAt: LocalDate, endAt: LocalDate): Long {
        val zone = ZoneId.systemDefault()
        val startDateTime = startAt.atStartOfDay(zone)
        val endDateTime = endAt.plusDays(1).atStartOfDay(zone)
        return store.values
            .count { it.refUserId == userId && it.orderedAt >= startDateTime && it.orderedAt < endDateTime }
            .toLong()
    }

    override fun findAll(): List<Order> {
        return store.values.toList()
    }
}
