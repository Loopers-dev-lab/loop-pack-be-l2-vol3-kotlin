package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderItemModel
import org.springframework.data.jpa.repository.JpaRepository

interface OrderItemJpaRepository : JpaRepository<OrderItemModel, Long> {
    fun findAllByOrderId(orderId: Long): List<OrderItemModel>
}
