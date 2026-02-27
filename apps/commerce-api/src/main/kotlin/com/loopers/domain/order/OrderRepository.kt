package com.loopers.domain.order

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.time.ZonedDateTime

interface OrderRepository {
    fun findById(id: Long): OrderModel?
    fun findAll(pageable: Pageable): Slice<OrderModel>
    fun findAllByUserIdAndCreatedAtBetween(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
        pageable: Pageable,
    ): Slice<OrderModel>
    fun save(order: OrderModel): OrderModel
}
