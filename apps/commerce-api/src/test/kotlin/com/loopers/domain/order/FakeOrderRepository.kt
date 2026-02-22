package com.loopers.domain.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

class FakeOrderRepository : OrderRepository {
    private val store = mutableListOf<OrderModel>()
    private var idSequence = 1L

    override fun save(order: OrderModel): OrderModel {
        val idField = order.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(order, idSequence++)

        val now = ZonedDateTime.now()
        order.javaClass.superclass.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(order, now)
        }
        order.javaClass.superclass.getDeclaredField("updatedAt").apply {
            isAccessible = true
            set(order, now)
        }

        store.add(order)
        return order
    }

    override fun findById(id: Long): OrderModel? {
        return store.find { it.id == id }
    }

    override fun findAllByMemberIdAndOrderedAtBetween(
        memberId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<OrderModel> {
        return store.filter {
            it.memberId == memberId &&
                !it.orderedAt.isBefore(startAt) &&
                !it.orderedAt.isAfter(endAt)
        }
    }

    override fun findAll(pageable: Pageable): Page<OrderModel> {
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, store.size)
        val content = if (start < store.size) store.subList(start, end) else emptyList()
        return PageImpl(content, pageable, store.size.toLong())
    }
}
