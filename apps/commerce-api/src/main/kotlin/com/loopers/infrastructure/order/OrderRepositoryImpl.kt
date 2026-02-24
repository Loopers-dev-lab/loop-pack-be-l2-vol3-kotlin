package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {

    override fun save(order: Order): Order {
        return orderJpaRepository.save(order)
    }

    override fun findByUserIdAndCreatedAtBetween(userId: Long, startAt: LocalDateTime, endAt: LocalDateTime): List<Order> {
        val zoneId = ZoneId.systemDefault()
        return orderJpaRepository.findByUserIdAndCreatedAtBetween(
            userId,
            startAt.atZone(zoneId),
            endAt.atZone(zoneId),
        )
    }
}
