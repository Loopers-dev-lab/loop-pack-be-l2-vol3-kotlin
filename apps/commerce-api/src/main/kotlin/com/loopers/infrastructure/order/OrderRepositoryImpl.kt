package com.loopers.infrastructure.order

import com.loopers.domain.PageResult
import com.loopers.domain.order.model.Order
import com.loopers.domain.order.repository.OrderRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderEntity, Long> {
    fun findAllByRefUserIdAndCreatedAtBetween(
        refUserId: Long,
        from: ZonedDateTime,
        to: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderEntity>
}

@Repository
class OrderRepositoryImpl(
    private val jpa: OrderJpaRepository,
) : OrderRepository {

    override fun save(order: Order): Order {
        return jpa.save(OrderEntity.fromDomain(order)).toDomain()
    }

    override fun findById(id: Long): Order? {
        return jpa.findById(id).orElse(null)?.toDomain()
    }

    override fun findAllByUserId(
        userId: Long,
        from: ZonedDateTime,
        to: ZonedDateTime,
        page: Int,
        size: Int,
    ): PageResult<Order> {
        val pageable = PageRequest.of(page, size)
        val result = jpa.findAllByRefUserIdAndCreatedAtBetween(userId, from, to, pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }

    override fun findAll(page: Int, size: Int): PageResult<Order> {
        val pageable = PageRequest.of(page, size)
        val result = jpa.findAll(pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }
}
