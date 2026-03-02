package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

class FakeOrderRepository : OrderRepository {

    private val store = mutableListOf<Order>()
    private var idSequence = 1L

    override fun save(order: Order): Order {
        if (order.id == 0L) {
            setEntityId(order, idSequence++)
        }
        store.add(order)
        return order
    }

    override fun findById(id: Long): Order? {
        return store.find { it.id == id && it.deletedAt == null }
    }

    override fun findAll(pageable: Pageable): Page<Order> {
        val filtered = store.filter { it.deletedAt == null }
        return toPage(filtered, pageable)
    }

    override fun findByUserIdAndCreatedAtBetween(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<Order> {
        return store.filter {
            it.deletedAt == null &&
                it.userId == userId &&
                !it.createdAt.isBefore(startAt) &&
                !it.createdAt.isAfter(endAt)
        }
    }

    private fun toPage(list: List<Order>, pageable: Pageable): Page<Order> {
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, list.size)
        val content = if (start <= list.size) list.subList(start, end) else emptyList()
        return PageImpl(content, pageable, list.size.toLong())
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
