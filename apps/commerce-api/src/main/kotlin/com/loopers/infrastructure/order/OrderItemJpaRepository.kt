package com.loopers.infrastructure.order

import com.loopers.domain.order.entity.OrderItem
import org.springframework.data.jpa.repository.JpaRepository

interface OrderItemJpaRepository : JpaRepository<OrderItem, Long> {
    fun findAllByRefOrderId(refOrderId: Long): List<OrderItem>
    fun findAllByRefOrderIdIn(refOrderIds: List<Long>): List<OrderItem>
}
