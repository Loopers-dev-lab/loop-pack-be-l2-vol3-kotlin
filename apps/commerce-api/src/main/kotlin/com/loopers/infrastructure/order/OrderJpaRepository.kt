package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OrderJpaRepository : JpaRepository<OrderModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): OrderModel?
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<OrderModel>
}
