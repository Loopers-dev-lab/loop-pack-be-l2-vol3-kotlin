package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository
) : OrderRepository {
    override fun save(order: OrderModel): OrderModel {
        return orderJpaRepository.save(order)
    }

    override fun findById(id: Long): OrderModel? {
        return orderJpaRepository.findById(id).orElse(null)
    }

    override fun findAll(pageable: Pageable): Page<OrderModel> {
        return orderJpaRepository.findAll(pageable)
    }

    override fun findAllByUserId(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderModel> {
        return orderJpaRepository.findAllByUserIdAndCreatedAtBetween(userId, startAt, endAt, pageable)
    }
}
