package com.loopers.domain.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: Long): Order?
    fun findAll(pageable: Pageable): Page<Order>
    fun findByUserIdAndCreatedAtBetween(userId: Long, startAt: ZonedDateTime, endAt: ZonedDateTime): List<Order>
}
