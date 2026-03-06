package com.loopers.domain.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

interface OrderRepository {
    fun save(order: Order): Order
    fun findByIdAndDeletedAtIsNull(id: Long): Order?
    fun findAllByUserIdAndCreatedAtBetween(userId: Long, startAt: ZonedDateTime, endAt: ZonedDateTime): List<Order>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Order>
}
