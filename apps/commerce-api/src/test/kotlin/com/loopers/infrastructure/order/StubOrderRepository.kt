package com.loopers.infrastructure.order

import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class StubOrderRepository : OrderRepository {

    private val store = ConcurrentHashMap<Long, Order>()
    private val idSequence = AtomicLong(1)

    override fun save(order: Order): Order {
        val id = order.id ?: idSequence.getAndIncrement()
        val saved = Order.retrieve(
            id = id,
            userId = order.userId,
            idempotencyKey = order.idempotencyKey,
            status = order.status,
            items = order.items,
            createdAt = order.createdAt ?: ZonedDateTime.now(),
        )
        store[id] = saved
        return saved
    }

    override fun findById(id: Long): Order? = store[id]

    override fun findByIdAndUserId(id: Long, userId: Long): Order? =
        store[id]?.takeIf { it.userId == userId }

    override fun findAllByUserId(
        userId: Long,
        from: ZonedDateTime?,
        to: ZonedDateTime?,
        pageRequest: PageRequest,
    ): PageResponse<Order> {
        val filtered = store.values
            .filter { it.userId == userId }
            .filter { order ->
                val createdAt = order.createdAt ?: return@filter true
                (from == null || !createdAt.isBefore(from)) &&
                    (to == null || createdAt.isBefore(to))
            }
            .sortedByDescending { it.createdAt }

        val start = pageRequest.page * pageRequest.size
        val content = filtered.drop(start).take(pageRequest.size)

        return PageResponse(
            content = content,
            totalElements = filtered.size.toLong(),
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }

    override fun findAll(pageRequest: PageRequest): PageResponse<Order> {
        val all = store.values.toList()
        val start = pageRequest.page * pageRequest.size
        val content = all.drop(start).take(pageRequest.size)

        return PageResponse(
            content = content,
            totalElements = all.size.toLong(),
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }

    override fun existsByIdempotencyKey(idempotencyKey: IdempotencyKey): Boolean =
        store.values.any { it.idempotencyKey == idempotencyKey }
}
