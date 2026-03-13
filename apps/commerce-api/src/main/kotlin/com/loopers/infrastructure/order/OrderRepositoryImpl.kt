package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {

    override fun save(order: OrderModel): OrderModel {
        return orderJpaRepository.save(order)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): OrderModel? {
        return orderJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<OrderModel> {
        return orderJpaRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable)
    }

    override fun findAllByDeletedAtIsNull(pageable: Pageable): Page<OrderModel> {
        return orderJpaRepository.findAllByDeletedAtIsNull(pageable)
    }

    override fun findAllByUserIdAndCreatedAtBetween(
        userId: Long,
        startAt: LocalDate,
        endAt: LocalDate,
        pageable: Pageable,
    ): Page<OrderModel> {
        return orderJpaRepository.findAllByUserIdAndCreatedAtBetween(userId, startAt, endAt, pageable)
    }
}
