package com.loopers.domain.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface OrderRepository {
    fun save(order: OrderModel): OrderModel
    fun findByIdAndDeletedAtIsNull(id: Long): OrderModel?
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<OrderModel>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<OrderModel>
    fun findAllByUserIdAndCreatedAtBetween(
        userId: Long,
        startAt: LocalDate,
        endAt: LocalDate,
        pageable: Pageable,
    ): Page<OrderModel>
}
