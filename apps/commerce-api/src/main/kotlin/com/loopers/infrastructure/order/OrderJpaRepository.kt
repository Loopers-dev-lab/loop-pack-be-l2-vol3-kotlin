package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderModel, Long> {
    fun findAllByUserIdAndCreatedAtBetween(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderModel>
}
