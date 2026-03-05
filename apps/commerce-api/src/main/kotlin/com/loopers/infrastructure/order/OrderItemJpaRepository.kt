package com.loopers.infrastructure.order

import org.springframework.data.jpa.repository.JpaRepository

interface OrderItemJpaRepository : JpaRepository<OrderItemJpaModel, Long> {
    fun findAllByOrderId(orderId: Long): List<OrderItemJpaModel>
}
