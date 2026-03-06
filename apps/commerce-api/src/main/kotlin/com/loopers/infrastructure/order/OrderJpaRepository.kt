package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<Order, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Order?
    fun findAllByUserIdAndDeletedAtIsNullAndCreatedAtBetween(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<Order>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Order>
}
