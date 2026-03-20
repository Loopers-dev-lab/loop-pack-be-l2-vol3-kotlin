package com.loopers.application.order

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderRepository
import java.time.ZonedDateTime

class FakeOrderRepository : OrderRepository {
    private val store = mutableMapOf<Long, OrderModel>()
    private var idSequence = 1L

    override fun save(order: OrderModel): OrderModel {
        val now = ZonedDateTime.now()
        val saved = if (order.id == 0L) {
            order.copy(id = idSequence++, createdAt = now, updatedAt = now)
        } else {
            order.copy(updatedAt = now)
        }
        store[saved.id] = saved
        return saved
    }

    override fun findById(id: Long): OrderModel? {
        return store[id]
    }

    override fun findAllByMemberIdAndOrderedAtBetween(
        memberId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<OrderModel> {
        return store.values.filter {
            it.memberId == memberId &&
                !it.orderedAt.isBefore(startAt) &&
                !it.orderedAt.isAfter(endAt)
        }
    }

    override fun findAll(pageQuery: PageQuery): PageResult<OrderModel> {
        val all = store.values.toList()
        val start = pageQuery.page * pageQuery.size
        val end = minOf(start + pageQuery.size, all.size)
        val content = if (start < all.size) all.subList(start, end) else emptyList()
        return PageResult(
            content = content,
            totalElements = all.size.toLong(),
            totalPages = if (pageQuery.size > 0) (all.size + pageQuery.size - 1) / pageQuery.size else 0,
        )
    }
}
