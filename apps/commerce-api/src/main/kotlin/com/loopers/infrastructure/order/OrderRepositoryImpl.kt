package com.loopers.infrastructure.order

import com.loopers.domain.PageResult
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {

    override fun save(order: Order): Order {
        return orderJpaRepository.save(order)
    }

    override fun findById(id: Long): Order? {
        return orderJpaRepository.findById(id).orElse(null)
    }

    override fun findAllByUserId(
        userId: Long,
        from: ZonedDateTime,
        to: ZonedDateTime,
        page: Int,
        size: Int,
    ): PageResult<Order> {
        val pageable = PageRequest.of(page, size)
        val result = orderJpaRepository.findAllByRefUserIdAndCreatedAtBetween(userId, from, to, pageable)
        return PageResult(result.content, result.totalElements, page, size)
    }

    override fun findAll(page: Int, size: Int): PageResult<Order> {
        val pageable = PageRequest.of(page, size)
        val result = orderJpaRepository.findAll(pageable)
        return PageResult(result.content, result.totalElements, page, size)
    }
}
