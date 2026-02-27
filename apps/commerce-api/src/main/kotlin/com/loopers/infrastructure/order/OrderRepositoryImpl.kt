package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {
    override fun findById(id: Long): OrderModel? {
        return orderJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAll(pageable: Pageable): Slice<OrderModel> {
        return orderJpaRepository.findAllByDeletedAtIsNull(pageable)
    }

    override fun findAllByUserIdAndCreatedAtBetween(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
        pageable: Pageable,
    ): Slice<OrderModel> {
        return orderJpaRepository.findAllByUserIdAndCreatedAtBetweenAndDeletedAtIsNull(
            userId = userId,
            startAt = startAt,
            endAt = endAt,
            pageable = pageable,
        )
    }

    override fun save(order: OrderModel): OrderModel {
        return orderJpaRepository.save(order)
    }
}
