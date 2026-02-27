package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {
    override fun save(order: Order): Order {
        return orderJpaRepository.save(order)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): Order? {
        return orderJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAllByUserIdAndCreatedAtBetween(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<Order> {
        return orderJpaRepository.findAllByUserIdAndDeletedAtIsNullAndCreatedAtBetween(userId, startAt, endAt)
    }

    override fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Order> {
        return orderJpaRepository.findAllByDeletedAtIsNull(pageable)
    }
}
