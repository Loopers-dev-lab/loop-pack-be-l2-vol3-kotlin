package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrderItemJpaRepository : JpaRepository<OrderItem, Long> {

    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId")
    fun findByOrderId(@Param("orderId") orderId: Long): List<OrderItem>

    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId IN :orderIds")
    fun findByOrderIds(@Param("orderIds") orderIds: List<Long>): List<OrderItem>
}
