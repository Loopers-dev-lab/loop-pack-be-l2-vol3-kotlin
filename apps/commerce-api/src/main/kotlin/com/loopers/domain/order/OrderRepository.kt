package com.loopers.domain.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

interface OrderRepository {
    fun save(order: OrderModel): OrderModel
    fun findById(id: Long): OrderModel?
    fun findAll(pageable: Pageable): Page<OrderModel>
    fun findAllByUserId(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderModel>
}
