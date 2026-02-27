package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): OrderModel?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Slice<OrderModel>
    fun findAllByUserIdAndCreatedAtBetweenAndDeletedAtIsNull(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
        pageable: Pageable,
    ): Slice<OrderModel>
}
