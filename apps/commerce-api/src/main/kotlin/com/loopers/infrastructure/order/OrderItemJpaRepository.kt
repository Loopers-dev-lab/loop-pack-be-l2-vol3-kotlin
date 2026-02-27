package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderItemJpaRepository : JpaRepository<OrderItem, Long>
