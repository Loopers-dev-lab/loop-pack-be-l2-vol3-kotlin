package com.loopers.infrastructure.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): OrderEntity?
    fun findByIdAndUserIdAndDeletedAtIsNull(id: Long, userId: Long): OrderEntity?
    fun findAllByUserIdAndCreatedAtBetweenAndDeletedAtIsNull(
        userId: Long,
        from: ZonedDateTime,
        to: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderEntity>
    fun findAllByCreatedAtBetweenAndDeletedAtIsNull(
        from: ZonedDateTime,
        to: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderEntity>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<OrderEntity>
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
}
